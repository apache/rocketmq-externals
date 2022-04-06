#!/bin/sh
source /etc/profile
pid=$(jps -lvVm | grep rocketmq-exporter | awk '{print $1}')

if [ ! -z $pid ]; then
  kill -9 $pid
fi