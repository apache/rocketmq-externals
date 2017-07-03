#!/bin/bash

PROGRAM_NAME="org.apache.rocketmq.mysql.Replicator"
PIDS=`ps -ef | grep $PROGRAM_NAME | grep -v "grep" | awk '{print $2}'`

if [ -z $PIDS ]; then
    echo "No this process."
else
    echo "Find process is $PIDS."
fi

#####kill####
echo -e "Stopping the $PROGRAM_NAME...\c"
for PID in $PIDS ; do
    kill  $PID
done

echo "SUCCESS!"
