#!/bin/bash

# Stop and remove existed containers if any
docker rm -fv $(docker ps -a|awk '/rmq/ {print $1}')

# Wait till the existing containers are removed
sleep 5

if [ ! -d "`pwd`/data" ]; then
  mkdir -p "data"
fi

chown -R 3000:3000 data

# Run namesrv and broker
docker run -d -p 9876:9876 -v `pwd`/data/namesrv/logs:/home/rocketmq/logs -v `pwd`/data/namesrv/store:/home/rocketmq/store --name rmqnamesrv  rocketmqinc/rocketmq:4.3.2 sh mqnamesrv
docker run -d -p 10911:10911 -p 10909:10909 -v `pwd`/data/broker/logs:/home/rocketmq/logs -v `pwd`/data/broker/store:/home/rocketmq/store --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:4.3.2 sh mqbroker

# Test to produce messages
sh ./play-producer.sh