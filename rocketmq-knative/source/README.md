## RocketMQ Source
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Language](https://img.shields.io/badge/Language-Go-blue.svg)](https://golang.org/)


## Overview

RocketMQSource which is a separate Kubernetes custom resource fires a new event each time an event is published on an Apache RocketMQ Cluster.

## Quick Start

### build  RocketMQ Source

1 Clone the project on your Kubernetes cluster master node:

```
$ git clone https://github.com/apache/rocketmq-externals

$ cd rocketmq-externals/rocketmq-knative/source
```


2.build controller and adapter 

```
$ docker build -f Dockerfile.adapter -t rocketmqsource-adapter .
$ docker build -f Dockerfile.controller  -t rocketmqsource-controller  .
```


3 push image to docker hub

```
$ docker tag rocketmqsource-adapter   $dockerhub-user/rocketmqsource-adapter:$version-adapter
$ docker push $dockerhub-user/rocketmqsource-adapter:$version-adapter
$ docker tag rocketmqsource-adapter   $dockerhub-user/rocketmqsource-adapter:$version-adapter
$ docker push $dockerhub-user/rocketmqsource-adapter:$version-adapter
```

### Deploy RocketMQ Source

1 update the version of docker image
  
  ```
  $ vi config/500-controller.yaml
  ```
  
2 deploy the controller of rocketmqsource

```
$ kubectl apply -f 	300-rocketmqsource.yaml -f 400-controller-service.yaml -f 500-controller.yaml -f 600-istioegress.yaml -f 200-serviceaccount.yaml -f 201-clusterrole.yaml -f 202-clusterrolebinding.yaml

```

3 check the controller pod

```
$ kubectl -n knative-sources get pods
NAME                                  READY   STATUS    RESTARTS   AGE
rocketmqsource-controller-manager-0   1/1     Running   0          23h

```

