#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

## Main
if [ $# -lt 4 ]; then
    echo "Usage: sh $0 DATA_HOME ROCKETMQ_VERSION NAMESRV_ADDR CONF_FILE"
    exit -1
fi

DATA_HOME=$1
ROCKETMQ_VERSION=$2
NAMESRV_ADDR=$3
CONF_FILE=$4

## Show Env Setting
echo "ENV Setting: "
echo "  DATA_HOME=${DATA_HOME} ROCKETMQ_VERSION=${ROCKETMQ_VERSION}"
echo "  NAMESRV_ADDR=${NAMESRV_ADDR}"
echo "  CONF_FILE=${CONF_FILE}" 

## Check config file existing
if [ ! -f "${DATA_HOME}/conf/${CONF_FILE}" ]; then
    echo "You must ensure the broker config file [${DATA_HOME}/conf/${CONF_FILE}] is pre-defined!!!"
    exit -1
fi


# Start Broker
docker run -d  -v ${DATA_HOME}/logs:/home/rocketmq/logs -v ${DATA_HOME}/store:/home/rocketmq/store \
  -v ${DATA_HOME}/conf:/home/rocketmq/conf \
  --name rmqbroker \
  -e "NAMESRV_ADDR=${NAMESRV_ADDR}" \
  -p 10911:10911 -p 10912:10912 -p 10909:10909 \
  rocketmqinc/rocketmq:${ROCKETMQ_VERSION} \
  sh mqbroker -c /home/rocketmq/conf/${CONF_FILE}
