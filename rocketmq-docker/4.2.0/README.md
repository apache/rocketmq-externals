## Build and run RocketMQ with a single instance

This repository includes the following: 

- Dockerfiles by RocketMQ default setting. 

Please kindly pay attention that default setting would ask specific JVM capacity for RocketMQ containers successfully running, which could be found at `${ROCKETMQ_HOME}/bin/runserver.sh` and `${ROCKETMQ_HOME}/bin/runbroker.sh`;


- A customized sample for development usage. 

- Simple scripts for build and run RocketMQ containers.



### For Docker

Simply run: 

```
cd sample

./play.sh

```

### For docker-compose

For a single instance running on docker-compose:

1) Customize broker's Dockerfile, replace `sh mqbroker`  with `sh mqbroker -n namesrv:9876`.

2) Re-build default broker image, for example:

```
cd default

docker build -t apache/rocketmq-broker:4.2.0 --build-arg version=4.2.0 ./broker

```

3) Then build customized images and run containers with docker-compose commands:

```
cd sample

docker-compose -f docker-compose.yml build
docker-compose -f docker-compose.yml up -d

```

### For Kubernetes

For a single RocketMQ instance running on Kubernetes:

1) Customize broker's Dockerfile, replace `sh mqbroker`  with `sh mqbroker -n localhost:9876`.

2) Re-build default broker image, for example:

```
cd default

docker build -t apache/rocketmq-broker:4.2.0 --build-arg version=4.2.0 ./broker

```

3) Re-build customized broker image, for example:

```
cd sample

docker build -t apache/rocketmq-broker:4.2.0-k8s --build-arg version=4.2.0 ./broker

```

4) Then create the Kubernetes deployment accordingly.

```

kubectl create -f deployment.yaml

```
