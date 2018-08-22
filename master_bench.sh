#!/bin/bash

#Inspired by the original Flint benchmarking harness script 

#./master_bench.sh -b (kmeans|als|tsort) -k (0..10) -m (executor|worker|stress|none) 

origargs=$@

while [[ $# > 1 ]]
do
key="$1"

case $key in
    -b|--benchmark)
    BENCHMARK="$2"
    shift # past argument
    ;;
    -k|--tokill)
    TOKILL="$2"
    shift # past argument
    ;;
    -m|--method)
    METHOD="$2"
    shift # past argument
    ;;
    --default)
    DEFAULT=YES
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done


#EC2 specific 
SLAVES=/home/ubuntu/dspark/conf/slaves

MY_SPARK_HOME=/home/ubuntu/dspark

RESULTS_HOME=/home/ubuntu/results

EXECS_PER_WORKER=2 

function creat_result_dir() {

    #Useful to keep global expt counter for multiple trials etc 
    expt_counter="expt_counter"

    if [ ! -f $RESULTS_HOME/$expt_counter ]; then
	echo 0 > $expt_counter
    fi
    prev_expt_num=`cat $RESULTS_HOME/$expt_counter`
    new_expt_num=$(( $prev_expt_num+1 ))
    echo $new_expt_num > $RESULTS_HOME/$expt_counter 

    progdir="$BENCHMARK"_"$TOKILL"_"$METHOD"_"$new_expt_num"
    resultsdir=$RESULTS_HOME/$progdir
    mkdir $resultsdir
    if [ "$?" != "0" ]; then
	echo "Directory already exists, bye"
	exit 1
    fi
}

function pre_expt_cleanup() {
    echo "Killing stress-ngs"
    parallel-ssh -h $SLAVES pkill stress

    #echo "Removing worker dirs"
    #parallel-ssh -h $SLAVES "rm -rf $MY_SPARK_HOME/work/*"

    echo "Starting All workers just in case"
    $MY_SPARK_HOME/sbin/start-all.sh
}

function start_stressor() {
    stress-ng --vm-keep --vm-bytes 2g --vm $cpus_to_stress 2>/dev/null &
    stress_pid=$!
}

function start_logging() {
    date >> $resultsdir/start 
    date +%s >> $resultsdir/start_seconds

    echo `date` , "$origargs" >> $RESULTS_HOME/log  
    
}


creat_result_dir
echo $resultsdir created 

pre_expt_cleanup

benchmark_script=$MY_SPARK_HOME/bench_$BENCHMARK.sh 

if [ ! -f $benchmark_script ]; then
    echo "Benchmark file $benchmark_script not found, exiting"
    exit 3
fi

starttime=`date +%s`
outfile=$resultsdir/outfile
touch $outfile

nohup sh $benchmark_script  > $outfile 2>&1 &

echo "Job should be running, now sleeping"

if [ "$TOKILL" == 0 ];
then
    echo "Nothing to kill/stress, waiting for things to finish"

else
    echo "Something to kill, but sleeping 5 minutes first" 
    sleep 300 #XXX 60 just to speed up testing. change to 300 
    echo "Woken up to kill"
    
    case $METHOD in
	"executor")
	    echo "Killing executors"
	    execstokill=$(( $TOKILL*$EXECS_PER_WORKER )) 
	    sh $MY_SPARK_HOME/killmechtest.sh $execstokill
	    #TODO : Convert this script please
	    ;;
	"worker")
	    echo "Killing entire worker"
	    
	    slavestokill=`cat $SLAVES | head -n $TOKILL`

	    parallel-ssh -H "$slavestokill" "$MY_SPARK_HOME/sbin/stop-slave.sh"

	    echo "Killed $slavestokill" 
	    ;;
	"stress")
	    echo "Inducing stress"
	    #XXX DANGER, only valid upto 50% deflation ACHTUNG 
	    slavestostress=`cat $SLAVES | head -n $TOKILL`

	    parallel-ssh -H "$slavestostress" stress-ng --vm-keep --vm-bytes 3.5g --vm 2 &

	    echo "Stressed $slavestostress"
	    ;;
	*)
	    ;;

    esac

    
fi

echo ">>>>>>>>>> NOW WAIT FOR EXPERIMENT TO FINISH >>>>>>>>>>>>>> "

while true 
do
    sleep 10
    curl -s http://localhost:4040/api/v1/applications > /dev/null
    if [ $? != 0 ]; then
	echo "API server not reachable, assuming exit"
	date >> $resultsdir/end
	
	td=$(($endtime-$starttime))
	echo $td >> $resultsdir/timediff

	exit
    fi
done
