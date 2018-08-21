#!/bin/bash
    #--master spark://172.31.30.119:6066  --deploy-mode cluster \
./bin/spark-submit \
     --master spark://172.31.30.119:7077 \
     --deploy-mode client \
     --class org.apache.spark.examples.mllib.DenseKMeans \
     --jars /home/ubuntu/dspark/examples/target/scala-2.11/jars/scopt_2.11-3.7.0.jar \
     /home/ubuntu/dspark/examples/target/original-spark-examples_2.11-2.3.1-SNAPSHOT.jar \
     -k 20 \
     --numIterations 500 \
     file:///hdfs/datasets/km1b.txt
#km10m.txt


#k
#numIterations
