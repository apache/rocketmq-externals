#!/bin/bash

RMQ_CONTAINER=$(docker ps -a|awk '/rmq/ {print $1}')
if [[ -n "$RMQ_CONTAINER" ]]; then
   echo "Removing RocketMQ Container..."
   docker rm -fv $RMQ_CONTAINER
   # Wait till the existing containers are removed
   sleep 5
fi

DLEDGER_NET=$(docker network ls |awk '/dledger-br/ {print $1}')
if [[ -n "$DLEDGER_NET" ]]; then
   echo "Removing DLedger Bridge network..."
   docker network rm $DLEDGER_NET
   # Wait till the existing networks are removed
   sleep 5
fi

if [ ! -d "`pwd`/data" ]; then
  mkdir -p "data"
fi

echo "Starting RocketMQ nodes..."

# Create network
docker network create --subnet=172.18.0.0/16 dledger-br

# Start nameserver
docker run --net dledger-br --ip 172.18.0.11  -d -p 9876:9876 -v `pwd`/data/namesrv/logs:/home/rocketmq/logs -v `pwd`/data/namesrv/store:/home/rocketmq/store --name rmqnamesrv  rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqnamesrv

# Start Brokers
docker run --net dledger-br --ip 172.18.0.12 -d -p 30911:30911 -p 30909:30909 -v `pwd`/data/broker0/logs:/home/rocketmq/logs -v `pwd`/data/broker0/store:/home/rocketmq/store -v `pwd`/data/broker0/conf/dledger:/opt/rocketmq-ROCKETMQ_VERSION/conf/dledger --name rmqbroker --link rmqnamesrv:namesrv -e "MAX_POSSIBLE_HEAP=200000000" -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqbroker  -c  ../conf/dledger/broker.conf
docker run --net dledger-br --ip 172.18.0.13 -d -p 30921:30921 -p 30919:30919 -v `pwd`/data/broker1/logs:/home/rocketmq/logs -v `pwd`/data/broker1/store:/home/rocketmq/store -v `pwd`/data/broker1/conf/dledger:/opt/rocketmq-ROCKETMQ_VERSION/conf/dledger --name rmqbroker1 --link rmqnamesrv:namesrv -e "MAX_POSSIBLE_HEAP=200000000" -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqbroker  -c  ../conf/dledger/broker.conf
docker run --net dledger-br --ip 172.18.0.14 -d -p 30931:30931 -p 30929:30929 -v `pwd`/data/broker2/logs:/home/rocketmq/logs -v `pwd`/data/broker2/store:/home/rocketmq/store -v `pwd`/data/broker2/conf/dledger:/opt/rocketmq-ROCKETMQ_VERSION/conf/dledger --name rmqbroker2 --link rmqnamesrv:namesrv -e "MAX_POSSIBLE_HEAP=200000000" -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:ROCKETMQ_VERSION sh mqbroker  -c  ../conf/dledger/broker.conf

# Servive unavailable when not ready
# sleep 20

# Produce messages
# sh ./play-producer.sh
