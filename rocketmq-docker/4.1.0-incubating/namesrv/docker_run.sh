#!/bin/bash
sudo docker run -d -p 9876:9876 --name rmqnamesrv  apache/incubator-rocketmq-namesrv:4.1.0-incubating
