#!/bin/bash

docker build -t apache/rocketmq-namesrv:4.2.0-dev ./namesrv

docker build -t apache/rocketmq-broker:4.2.0-dev ./broker

docker run -d -p 9876:9876 --name rmqnamesrv  apache/rocketmq-namesrv:4.2.0-dev

docker run -d -p 10911:10911 -p 10909:10909 --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" apache/rocketmq-broker:4.2.0-dev