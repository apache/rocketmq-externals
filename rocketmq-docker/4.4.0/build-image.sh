#!/bin/bash
if [ -z "${ROCKETMQ_VERSION}" ]
then
  ROCKETMQ_VERSION="4.4.0"
fi

# Build rocketmq
docker build --no-cache -t rocketmqinc/rocketmq:${ROCKETMQ_VERSION} --build-arg version=${ROCKETMQ_VERSION} .
