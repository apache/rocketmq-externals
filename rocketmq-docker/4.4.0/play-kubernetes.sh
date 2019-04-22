#!/bin/bash

RMQ_DEPLOYMENT=$(kubectl get deployments|awk '/rocketmq-k8s/ {print $1}')
if [[ -n "$RMQ_DEPLOYMENT" ]]; then
   kubectl delete -f kubernetes/deployment.yaml
   sleep 3
fi

# Run namesrv and broker on your Kubernetes cluster
kubectl create -f kubernetes/deployment.yaml
