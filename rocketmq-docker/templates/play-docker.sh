#!/bin/bash

RMQ_CONTAINER=$(docker ps -a|awk '/rmq/ {print $1}')
if [[ -n "$RMQ_CONTAINER" ]]; then
   echo "Removing RocketMQ Container..."
   docker rm -fv $RMQ_CONTAINER
   # Wait till the existing containers are removed
   sleep 5
fi

if [ ! -d "`pwd`/data" ]; then
  mkdir -p "data"
fi

echo "Starting RocketMQ nodes..."

# Start nameserver
# Start nameserver
docker run -d -v `pwd`/data/namesrv/logs:/home/rocketmq/logs -v `pwd`/data/namesrv/store:/home/rocketmq/store --name rmqnamesrv rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqnamesrv

# Start Broker
docker run -d  -v `pwd`/data/broker/logs:/home/rocketmq/logs -v `pwd`/data/broker/store:/home/rocketmq/store --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqbroker

# Servive unavailable when not ready
# sleep 20

# Produce messages
# sh ./play-producer.sh
