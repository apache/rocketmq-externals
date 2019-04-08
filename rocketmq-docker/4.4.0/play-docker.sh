#!/bin/bash

# Stop and remove existed containers if any
docker rm -fv $(docker ps -a|awk '/rmq/ {print $1}')

# Wait till the existing containers are removed
sleep 5

# Run namesrv and broker
docker run -d -p 9876:9876 -v `pwd`/data/namesrv/logs:/root/logs -v `pwd`/data/namesrv/store:/root/store --name rmqnamesrv  rocketmqinc/rocketmq:4.4.0 sh mqnamesrv
docker run -d -p 10911:10911 -p 10909:10909 -v `pwd`/data/broker/logs:/root/logs -v `pwd`/data/broker/store:/root/store --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:4.4.0 sh mqbroker

# Test to produce messages
sh ./play-producer.sh