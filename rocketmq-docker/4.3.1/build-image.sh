#!/bin/bash
if [ -z "${ROCKETMQ_VERSION}" ]
then
  ROCKETMQ_VERSION="4.3.1"
fi

# Build rocketmq
docker build -t rocketmqinc/rocketmq:${ROCKETMQ_VERSION} --build-arg version=${ROCKETMQ_VERSION} .