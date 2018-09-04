#!/bin/bash

CURDIR=`cd $(dirname $0) && pwd`
BASEDIR=$(dirname ${CURDIR})
MAINCLASS=org.apache.rocketmq.iot.MQTTBridge
VERSION=0.0.1-SNAPSHOT
JAR=rocketmq-iot-bridge-${VERSION}.jar
PID_FILE=${CURDIR}/.server.pid

function _start() {
    java -cp ${BASEDIR}/target/${JAR} ${MAINCLASS} &
    echo $! > ${CURDIR}/.server.pid
    echo "RocketMQ-IoT-Bridge started ..."
}

function _stop() {
    if [ ! -f ${PID_FILE} ]; then
        echo "RocketMQ-IoT-Bridge is not started!"
        exit 1
    fi
    cat ${PID_FILE} | xargs kill -9
    rm ${PID_FILE}
}

function _restart() {
    if [ -f ${PID_FILE} ]; then
        echo "Restart RocketMQ-IoT-Bridge"
        _stop
    else
        echo "RocketMQ-IoT-Bridge is not started, the server will be stared immediatey!"
    fi
    _start
}

COMMAND=$1

case ${COMMAND} in
    start)
        _start
        ;;
    stop)
        _stop
        ;;
    restart)
        _restart
        ;;
esac
