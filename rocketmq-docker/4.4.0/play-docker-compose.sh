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

# Run namesrv and broker
docker-compose -f ./docker-compose/docker-compose.yml up -d
