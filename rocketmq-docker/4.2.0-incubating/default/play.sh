#!/bin/bash

docker build -t apache/incubator-rocketmq-namesrv:4.2.0-incubating --build-arg version=4.2.0 ./namesrv

docker build -t apache/incubator-rocketmq-broker:4.2.0-incubating --build-arg version=4.2.0 ./broker

docker run -d -p 9876:9876 --name rmqnamesrv  apache/incubator-rocketmq-namesrv:4.2.0-incubating

docker run -d -p 10911:10911 -p 10909:10909 --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" apache/incubator-rocketmq-broker:4.2.0-incubating