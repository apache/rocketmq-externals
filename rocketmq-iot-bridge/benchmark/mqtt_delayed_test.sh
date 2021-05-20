#!/bin/bash

CURDIR=`cd $(dirname $0) && pwd`
BASEDIR=$(dirname ${CURDIR})
MAINCLASS=org.apache.rocketmq.iot.benchmark.MqttDelayedTest
VERSION=0.0.1-SNAPSHOT
JAR=rmq-mqtt-${VERSION}.jar

broker=tcp://staging-cnbj2-rmq-mqtt.api.xiaomi.net:1883
password=123456
productInterval=100
timeout=3600

java -cp ${BASEDIR}/target/${JAR} ${MAINCLASS} ${broker} ${password} ${productInterval} ${timeout}
