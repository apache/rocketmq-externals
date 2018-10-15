#!/bin/bash

# Build base image
docker build -t apache/rocketmq-base:4.3.0 --build-arg version=4.3.0 ./rocketmq-base

# Build namesrv and broker
docker-compose -f docker-compose/docker-compose.yml build

# Run namesrv and broker
docker-compose -f docker-compose/docker-compose.yml up -d