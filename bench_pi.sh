#!/bin/bash

./bin/spark-submit --master spark://172.31.30.119:6066 --class org.apache.spark.examples.SparkPi  --deploy-mode cluster /home/ubuntu/dspark/examples/target/original-spark-examples_2.11-2.3.1-SNAPSHOT.jar 100000

