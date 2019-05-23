#!/bin/bash

if [ ! -d "`pwd`/data" ]; then
  mkdir -p "data"
fi

# Run nameserver and broker on your Kubernetes cluster
kubectl apply -f kubernetes/deployment.yaml
