#!/bin/bash
if [ $# -lt 1 ]; then
    echo "Usage: sh $0 version#"
    exit -1
fi

ROCKETMQ_VERSION=${1}
if [ -z "${ROCKETMQ_VERSION}" ]
then
  ROCKETMQ_VERSION="4.5.0"
fi

# Build rocketmq
docker build --no-cache -t rocketmqinc/rocketmq:${ROCKETMQ_VERSION} --build-arg version=${ROCKETMQ_VERSION} .
