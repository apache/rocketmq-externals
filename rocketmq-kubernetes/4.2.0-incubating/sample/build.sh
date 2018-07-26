#!/bin/bash

sudo docker build -t huanwei/rocketmq-namesrv:4.2.0-dev --build-arg version=4.2.0 ./namesrv

sudo docker build -t huanwei/rocketmq-broker:4.2.0-dev --build-arg version=4.2.0 ./broker