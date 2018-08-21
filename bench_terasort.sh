#!/bin/bash
    #--master spark://172.31.30.119:6066  --deploy-mode cluster \
./bin/spark-submit \
     --master spark://172.31.30.119:7077 \
     --deploy-mode client \
     --class com.github.ehiggs.spark.terasort.TeraGen \
     /home/ubuntu/dspark/examples/spark-terasort-1.1-SNAPSHOT-jar-with-dependencies.jar \
     2G s3a://spark-data-sample/sort

#k
#numIterations
