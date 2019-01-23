#!/bin/bash
if [ -z "${ROCKETMQ_VERSION}" ]
then
  ROCKETMQ_VERSION="4.3.1"
fi

# Save current dir
CURRENT_DIR="$(dirname $0)"

# Change to dir of Dockerfile
pushd ${CURRENT_DIR}

# Build rocketmq
docker build -t rocketmqinc/rocketmq:${ROCKETMQ_VERSION} --build-arg version=${ROCKETMQ_VERSION} .

# Change back to current dir
popd