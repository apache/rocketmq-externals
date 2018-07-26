# Apache RocketMQ For Kubrenetes

Apache RocketMQ for Kubernetes provides Dockerfile, bash scripts and sample yaml for building RocketMQ service on Kubernetes.

This repository includes a sample for development usage, which should resolve [issue 78](https://github.com/apache/rocketmq-externals/issues/78).


## Prequirement

The sample was run and tested on Kubernetes 1.9. Any Kubernetes versions above 1.6+ which support `Development` resource should be OK.


## Build Docker Images and Run with Docker:

For example, build the default images:

```
sh cd default

sh build.sh

```

`Note`: Make sure the scheduled node has sufficient Memory as the default namesrv and broker need.

For development usage, build the images for development usage(the JVM parameters are adjusted during namesrv and broker bootstraping).

```
sh cd sample

sh build.sh

```

## Run On Kubernetes:

For example, run the development usage RocketMQ on Kubernetes:

```
cd sample

kubectl create -f development.yaml
```
