#!/bin/bash

appid=`curl http://localhost:4040/api/v1/applications | jq '.[-1].id'`

tempid="${appid%\"}"
tempid="${tempid#\"}"

echo $tempid 

DRY=0
DRY=$1

curl http://localhost:4040/api/v1/applications/$tempid/executors

curl http://localhost:4040/api/v1/applications/$tempid/exec-id-test 

curl http://localhost:4040/api/v1/applications/$tempid/reclaim-executor?dryRun=$DRY



