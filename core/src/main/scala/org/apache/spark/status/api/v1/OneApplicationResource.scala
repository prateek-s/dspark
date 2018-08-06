/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.status.api.v1

import java.io.OutputStream
import java.util.{List => JList}
import java.util.zip.ZipOutputStream
import javax.ws.rs.{GET, Path, PathParam, Produces, QueryParam}
import javax.ws.rs.core.{MediaType, Response, StreamingOutput}

import scala.util.control.NonFatal

import org.apache.spark.JobExecutionStatus
import org.apache.spark.ui.SparkUI

@Produces(Array(MediaType.APPLICATION_JSON))
private[v1] class AbstractApplicationResource extends BaseAppResource {

  @GET
  @Path("jobs")
  def jobsList(@QueryParam("status") statuses: JList[JobExecutionStatus]): Seq[JobData] = {
    withUI(_.store.jobsList(statuses))
  }

  @GET
  @Path("jobs/{jobId: \\d+}")
  def oneJob(@PathParam("jobId") jobId: Int): JobData = withUI { ui =>
    try {
      ui.store.job(jobId)
    } catch {
      case _: NoSuchElementException =>
        throw new NotFoundException("unknown job: " + jobId)
    }
  }

  @GET
  @Path("executors")
  def executorList(): Seq[ExecutorSummary] = withUI(_.store.executorList(true))

  @GET
  @Path("allexecutors")
  def allExecutorList(): Seq[ExecutorSummary] = withUI(_.store.executorList(false))

  @Path("stages")
  def stages(): Class[StagesResource] = classOf[StagesResource]

  @GET
  @Path("storage/rdd")
  def rddList(): Seq[RDDStorageInfo] = withUI(_.store.rddList())

  @GET
  @Path("storage/rdd/{rddId: \\d+}")
  def rddData(@PathParam("rddId") rddId: Int): RDDStorageInfo = withUI { ui =>
    try {
      ui.store.rdd(rddId)
    } catch {
      case _: NoSuchElementException =>
        throw new NotFoundException(s"no rdd found w/ id $rddId")
    }
  }

  @GET
  @Path("environment")
  def environmentInfo(): ApplicationEnvironmentInfo = withUI(_.store.environmentInfo())

  @GET
  @Path("logs")
  @Produces(Array(MediaType.APPLICATION_OCTET_STREAM))
  def getEventLogs(): Response = {
    // Retrieve the UI for the application just to do access permission checks. For backwards
    // compatibility, this code also tries with attemptId "1" if the UI without an attempt ID does
    // not exist.
    try {
      withUI { _ => }
    } catch {
      case _: NotFoundException if attemptId == null =>
        attemptId = "1"
        withUI { _ => }
        attemptId = null
    }

    try {
      val fileName = if (attemptId != null) {
        s"eventLogs-$appId-$attemptId.zip"
      } else {
        s"eventLogs-$appId.zip"
      }

      val stream = new StreamingOutput {
        override def write(output: OutputStream): Unit = {
          val zipStream = new ZipOutputStream(output)
          try {
            uiRoot.writeEventLogs(appId, Option(attemptId), zipStream)
          } finally {
            zipStream.close()
          }

        }
      }

      Response.ok(stream)
        .header("Content-Disposition", s"attachment; filename=$fileName")
        .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
        .build()
    } catch {
      case NonFatal(e) =>
        Response.serverError()
          .entity(s"Event logs are not available for app: $appId.")
          .status(Response.Status.SERVICE_UNAVAILABLE)
          .build()
    }
  }


/*************************  Application Deflation ********************************/


  @GET
  @Path("try-deflate")
  def trydeflate() : Int = {
    return 23
  }

    def sacrificedExecutor() : (String, String) = {
    // Choose some executor to blacklist
    // Ideally, go through all and see which stages we are ok with impacting the most
    withUI { ui =>
      val statusStore = ui.store
      val execlist = statusStore.executorList(true)
      //TODO filter by blacklisted? 
      val victim = execlist.last
      val id = victim.id
      val hostPort = victim.hostPort 
      val host: String = hostPort.split(":")(0) //from storeTypes
      return (id, host)

      } //withUI
    }


  def isShuffle(sr:ShuffleReadMetrics, sw:ShuffleWriteMetrics): Boolean = {
    val total = sr.localBytesRead + sr.remoteBytesRead + sw.bytesWritten

    if (total > 0)
      return true

    return false 
  }

  def taskRunning(td: TaskData): Boolean = {
    val TASK_FINISHED_STATES = Set("FAILED", "KILLED", "SUCCESS")
    return !TASK_FINISHED_STATES.contains(td.status)
  }


  @GET
  @Path("shuffling")
  def shufflePending(): Int = {
    // Can we use TaskMetrics for this?
    // non-shuffle tasks will have shuffle read/write metrics all zero?
    withUI { ui =>
      val statusStore = ui.store
      for(stage <- statusStore.activeStages()) {
        val stageId = stage.stageId
        val attemptId = stage.attemptId
        val taskData = statusStore.taskList(stageId, attemptId,  maxTasks=100)
        // We need to make sure that this is a "current" task not completed yet
        for(td <- taskData) {
          //TODO XXX get correct task status string 
          if(taskRunning(td) && td.taskMetrics.isDefined) {
            val tm: TaskMetrics  = td.taskMetrics.get
            if(isShuffle(tm.shuffleReadMetrics, tm.shuffleWriteMetrics)) {
              return 1
            }
          }
        } //for tasks
      } //for stages

    } //with UI
    return 0
  }

  @GET
  @Path("num-stages")
  def numStages(): Int = {

    withUI { ui =>
      val statusStore = ui.store
      val activeStages = statusStore.activeStages()
      return activeStages.length 
    }
    //return ui.store.activeStages().length 
  }


  @GET
  @Path("stage-progress")
  def stageProgress() : Seq[Double] = {
    //In most(?) cases, stages are bookended by shuffle operations.
    //The goal is to determine how far away we are from a shuffle.
    //Thus, we can use the ratio of {tasks completed}/{total tasks} to infer this

    withUI { ui =>
      val statusStore = ui.store
      val activeStages = statusStore.activeStages()
      var progress = 0
      activeStages.map( s => s.numCompleteTasks.toDouble/s.numTasks )
    }
  }


  //Input should be the executor ID, surely?
  def BlacklistExecutor(execId: String, host: String) = {
    // Need to also add for the taskset? Only place blacklist is being used?
    // Main task: get the blacklist tracker object handle

    withUI { ui =>
      val sc = ui.sc.get 
      val foo = sc.sparkUser 
      val sched = sc.taskScheduler
      //var _s = sc._pubsched 
      //val btracker = sc.TaskSchedulerImpl.blacklistTrackerOpt
      val bt = sc.getblt 
      bt.updateBlacklistForDeflation(host, execId)
    }

  }


  @GET
  @Path("reclaim-executors")
  def reclaimExecutors(num: Int): Int = {
    //Give number of executors to sacrifice and reclaim 
    //The main top-level call.
    //Stash everything in here. Can call different policies to choose executors differently? 

  }


  /******************************************************************************/

  /**
   * This method needs to be last, otherwise it clashes with the paths for the above methods
   * and causes JAX-RS to not find things.
   */
  @Path("{attemptId}")
  def applicationAttempt(): Class[OneApplicationAttemptResource] = {
    if (attemptId != null) {
      throw new NotFoundException(httpRequest.getRequestURI())
    }
    classOf[OneApplicationAttemptResource]
  }

}

private[v1] class OneApplicationResource extends AbstractApplicationResource {

  @GET
  def getApp(): ApplicationInfo = {
    val app = uiRoot.getApplicationInfo(appId)
    app.getOrElse(throw new NotFoundException("unknown app: " + appId))
  }

}

private[v1] class OneApplicationAttemptResource extends AbstractApplicationResource {

  @GET
  def getAttempt(): ApplicationAttemptInfo = {
    uiRoot.getApplicationInfo(appId)
      .flatMap { app =>
        app.attempts.filter(_.attemptId == attemptId).headOption
      }
      .getOrElse {
        throw new NotFoundException(s"unknown app $appId, attempt $attemptId")
      }
  }

}
