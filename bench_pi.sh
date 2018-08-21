#!/bin/bash

./bin/spark-submit --master spark://172.31.30.119:7077 --class org.apache.spark.examples.SparkPi  --deploy-mode client /home/ubuntu/dspark/examples/target/original-spark-examples_2.11-2.3.1-SNAPSHOT.jar 1234

