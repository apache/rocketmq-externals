#!/bin/bash

CURDIR=`cd $(dirname $0) && pwd`
BASEDIR=$(dirname ${CURDIR})
MAINCLASS=org.apache.rocketmq.iot.example.MqttSampleProducer
VERSION=0.0.1-SNAPSHOT
JAR=rocketmq-iot-bridge-${VERSION}.jar
java -cp "${BASEDIR}/target/${JAR}" ${MAINCLASS}
