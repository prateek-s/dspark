#!/bin/bash

exec /home/ubuntu/dspark/bin/spark-submit \
    --class org.apache.spark.examples.SparkPi \
    --master spark://localhost:7077 \
    --deploy-mode cluster \ 
     --num-executors 3 \ 
     file:///home/ubuntu/dspark/examples/target/spark-examples_2.11-2.3.1-SNAPSHOT-sources.jar \ 
     100000
