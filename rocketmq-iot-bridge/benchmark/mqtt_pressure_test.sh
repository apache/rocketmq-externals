#!/bin/bash

CURDIR=`cd $(dirname $0) && pwd`
BASEDIR=$(dirname ${CURDIR})
MAINCLASS=org.apache.rocketmq.iot.benchmark.MqttPressureTest
VERSION=0.0.1-SNAPSHOT
JAR=rmq-mqtt-${VERSION}.jar

broker=tcp://staging-cnbj2-rmq-mqtt.api.xiaomi.net:1883
password=123456
taskNumber=5
rmqTopic=mqtt_01
productInterval=60000
msg="MSG:{\"pushType\":\"pull\",\"pushKey\":\"paySuccess\",\"msg\":\"\",\"data\":{\"orderId\":\"SA1208241001033021\",\"payId\":\"PA1208211000659011\",\"isMixed\":true,\"preSaleType\":0,\"status\":40,\"payType\":\"107\",\"payCategory\":\"3004\"}}"
timeout=3600

java -cp ${BASEDIR}/target/${JAR} ${MAINCLASS} ${broker} ${password} ${taskNumber} ${rmqTopic} ${productInterval} ${msg} ${timeout}
