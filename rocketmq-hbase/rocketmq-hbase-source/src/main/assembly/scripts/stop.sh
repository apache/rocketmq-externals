#!/usr/bin/env bash

PROGRAM_NAME="org.apache.rocketmq.hbase.source.RocketMQSource"
PIDS=`ps -ef | grep $PROGRAM_NAME | grep -v "grep" | awk '{print $2}'`

if [ -z $PIDS ]; then
    echo "$PROGRAM_NAME is not running."
else
    echo "$PROGRAM_NAME pids are $PIDS."
fi

#####kill####
echo -e "Stopping the $PROGRAM_NAME...\c"
for PID in $PIDS ; do
    kill  $PID
done

echo "SUCCESS!"
