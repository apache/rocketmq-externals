# Apache RocketMQ Docker module

Apache RocketMQ Docker module provides Dockerfiles and scripts for RocketMQ.

This repository includes the following: 

1. Dockerfile and scripts for RocketMQ images;
2. Dockerfile and scripts for RocketMQ run in following 3 scenarios:
- RocketMQ runs on single Docker daemon;
- RocketMQ runs with docker-compose;
- RocketMQ runs on Kubernetes.

## Quick start: Build and run RocketMQ with a single instance

### For Docker

Run: 

```
cd 4.3.0

./play-docker.sh

```

### For docker-compose

Run:

```
cd 4.3.0

./play-docker-compose.sh

```


### For Kubernetes

Run:

```
cd 4.3.0

./play-kubernetes.sh

```

## How to verify if your RocketMQ broker works

### Verify with Docker and docker-compose

1. Use `docker ps|grep rmqbroker` to find your RocketMQ broker container id, for example:
```
huandeMacBook-Pro:4.3.0 huan$ docker ps|grep rmqbroker
63950574b491        apache/rocketmq:4.3.0   "sh mqbroker"       9 minutes ago       Up 9 minutes        0.0.0.0:10909->10909/tcp, 9876/tcp, 0.0.0.0:10911->10911/tcp   rmqbroker
```

2. Use `docker exec -it {container_id} ./mqadmin clusterList -n {nameserver_ip}:9876` to verify if RocketMQ broker works, for example:
```
huandeMacBook-Pro:4.3.0 huan$ docker exec -it 63950574b491 ./mqadmin clusterList -n 192.168.43.56:9876
OpenJDK 64-Bit Server VM warning: ignoring option PermSize=128m; support was removed in 8.0
OpenJDK 64-Bit Server VM warning: ignoring option MaxPermSize=128m; support was removed in 8.0
#Cluster Name     #Broker Name            #BID  #Addr                  #Version                #InTPS(LOAD)       #OutTPS(LOAD) #PCWait(ms) #Hour #SPACE
DefaultCluster    63950574b491            0     172.17.0.3:10911       V4_3_0                   0.00(0,0ms)         0.00(0,0ms)          0 429398.92 -1.0000

```

### Verify with Kubernetes

1. Use `kubectl get pods|grep rocketmq` to find your RocketMQ broker Pod id, for example:
```
[root@k8s-master rocketmq]# kubectl get pods |grep rocketmq
rocketmq-7697d9d574-b5z7g             2/2       Running       0          2d
```

2. Use `kubectl -n {namespace} exec -it {pod_id} -c broker bash` to login the broker pod, for example:
```
[root@k8s-master rocketmq]# kubectl -n default exec -it  rocketmq-7697d9d574-b5z7g -c broker bash
[root@rocketmq-7697d9d574-b5z7g bin]# 
```

3. Use `mqadmin clusterList -n {nameserver_ip}:9876` to verify if RocketMQ broker works, for example:
```
[root@rocketmq-7697d9d574-b5z7g bin]# ./mqadmin clusterList -n localhost:9876
OpenJDK 64-Bit Server VM warning: ignoring option PermSize=128m; support was removed in 8.0
OpenJDK 64-Bit Server VM warning: ignoring option MaxPermSize=128m; support was removed in 8.0
#Cluster Name     #Broker Name            #BID  #Addr                  #Version                #InTPS(LOAD)       #OutTPS(LOAD) #PCWait(ms) #Hour #SPACE
DefaultCluster    rocketmq-7697d9d574-b5z7g  0     192.168.196.14:10911   V4_3_0                   0.00(0,0ms)         0.00(0,0ms)          0 429399.44 -1.0000

```

So you will find it works, enjoy!