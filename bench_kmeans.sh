#!/bin/bash
    #--master spark://172.31.30.119:6066  --deploy-mode cluster \
./bin/spark-submit \
     --master spark://172.31.30.119:6066 \
     --deploy-mode cluster \
     --class org.apache.spark.examples.mllib.DenseKMeans \
     --jars /home/ubuntu/dspark/examples/target/scala-2.11/jars/scopt_2.11-3.7.0.jar \
     /home/ubuntu/dspark/examples/target/original-spark-examples_2.11-2.3.1-SNAPSHOT.jar \
     -k 20 --numIterations 300 /hdfs/km10m.txt 

#k
#numIterations
