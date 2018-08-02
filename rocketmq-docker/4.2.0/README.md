## Build and run RocketMQ with a single instance

This repository includes the following: 

1. Dockerfile and scripts for RocketMQ base image;
2. Dockerfile and scripts for RocketMQ run in following 3 scenarios:
- RocketMQ runs on single Docker daemon;
- RocketMQ runs with docker-compose;
- RocketMQ runs on Kubernetes.

### For Docker

Run: 

```

./play-docker.sh

```

### For docker-compose

Run:

```

./play-docker-compose.sh

```


### For Kubernetes

Run:

```

./play-kubernetes.sh

```
