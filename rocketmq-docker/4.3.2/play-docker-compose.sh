#!/bin/bash

# Stop and remove existed containers if any
docker rm -f $(docker ps -a|awk '/rocketmqinc\/rocketmq/ {print $1}')

# Run namesrv and broker
docker-compose -f ./docker-compose/docker-compose.yml up -d