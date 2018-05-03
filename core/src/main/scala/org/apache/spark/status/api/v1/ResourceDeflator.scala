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

import java.util.{List => JList}
import javax.ws.rs._
import javax.ws.rs.core.MediaType

import org.apache.spark.SparkException
import org.apache.spark.scheduler.StageInfo
import org.apache.spark.status.api.v1._
//import org.apache.spark.status.api.v1.TaskSorting._
import org.apache.spark.ui.SparkUI

import org.apache.spark.scheduler.StageInfo
import org.apache.spark.storage.StorageLevel
//import org.apache.spark.status.api.v1.StageData

import scala.collection.mutable.{Buffer, ListBuffer}

import org.apache.spark.status.AppStatusStore

import org.apache.spark.{JobExecutionStatus, SecurityManager, SparkConf, SparkContext}

/** Notes: Once we get a deflation target, decide on what to do 
  */


@Produces(Array(MediaType.APPLICATION_JSON))
private[v1] class ResourceDeflator extends BaseAppResource {

  //XXX This can be tricky. Can we pass the executor IDs and the resource to be reduced from each?
  // [executor-id:(cpu, mem)]
  def parseTarget(): String = {
   return ""  
  }


  @GET
  @Path("reclaim-frac")
  def reclaimFrac(): Int = {
    //Return what fraction of resources are we actually willing to sacrifice 
    //For now, maybe we can just specify a fraction, and then reclaim those executors if we are willing? 
    return 0 
  }

  @GET
  @Path("estimateRecomputation")
  def estimateRecomputation(): Int = {
    return 1
  }

  @GET
  @Path("shuffling")
  def shufflePending(): Int = {
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


  @GET
  @Path("task-deps")
  def taskDeps() {
  }


}
