#!/bin/bash
export OMS_RMQ_DIRECT_NAME_SRV=true
echo "run rumtime worker"
cd target/distribution/ && java -cp .:./conf/:./lib/* org.apache.rocketmq.connect.runtime.ConnectStartup -c conf/connect.conf

