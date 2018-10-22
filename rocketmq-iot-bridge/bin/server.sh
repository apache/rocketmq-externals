#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
