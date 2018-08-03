#!/bin/bash

# Build base image
docker build -t apache/rocketmq-base:4.2.0 --build-arg version=4.2.0 ./rocketmq-base

# Build namesrv and broker
docker build -t apache/rocketmq-namesrv:4.2.0-k8s ./kubernetes/rocketmq-namesrv
docker build -t apache/rocketmq-broker:4.2.0-k8s ./kubernetes/rocketmq-broker

# Then `docker tag` images, and `docker push` images to respective registry
# For example:
#docker tag apache/rocketmq-namesrv:4.2.0-k8s huanwei/rocketmq-namesrv:4.2.0-k8s
#docker tag apache/rocketmq-broker:4.2.0-k8s huanwei/rocketmq-broker:4.2.0-k8s
#docker push huanwei/rocketmq-namesrv:4.2.0-k8s
#docker push huanwei/rocketmq-broker:4.2.0-k8s

# Finally, run namesrv and broker on your Kubernetes cluster.
# For example:
#kubectl create -f kubernetes/deployment.yaml
