#!/bin/bash

./bin/spark-submit --master spark://172.31.30.119:7077 \
		   --driver-java-options "-Dlog4j.configuration=file:/home/ubuntu/dspark/conf/log4j.properties" \
		   --conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:/home/ubuntu/dspark/conf/log4j.properties" \
		   --class org.apache.spark.examples.mllib.MovieLensALS \
		   --deploy-mode client \
		   --jars /home/ubuntu/dspark/examples/target/scala-2.11/jars/scopt_2.11-3.7.0.jar \
		   /home/ubuntu/dspark/examples/target/original-spark-examples_2.11-2.3.1-SNAPSHOT.jar \
		   --numIterations 15 \
		   --lambda 0.1 \
		   file:///hdfs/datasets/als1b.txt
#		   file:///hdfs/datasets/ml-20m/ratings_colon.csv
#lambda 0.1

#ACHTUNG. High iterations results in stackoverflow error. Ouch 
#10: works OK
#100: StackOverflow 
