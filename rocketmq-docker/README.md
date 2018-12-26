# Apache RocketMQ Docker module

Apache RocketMQ Docker module provides Dockerfiles and scripts for RocketMQ.

This repository includes the following: 

1. Dockerfile and scripts for RocketMQ images;
2. Dockerfile and scripts for RocketMQ run in following 3 scenarios:
- RocketMQ runs on single Docker daemon;
- RocketMQ runs with docker-compose;
- RocketMQ runs on Kubernetes.


## Supported Docker and Kubernetes versions

The Docker images in this repository should support Docker version 1.12+, and Kubernetes version 1.9+.

### Well-tested Docker and Kubernetes Environments

```
[root@k8s-master ~]# docker version
Client:
 Version:         1.12.6
 API version:     1.24
 Package version: docker-1.12.6-71.git3e8e77d.el7.centos.1.x86_64
 Go version:      go1.8.3
 Git commit:      3e8e77d/1.12.6
 Built:           Tue Jan 30 09:17:00 2018
 OS/Arch:         linux/amd64

Server:
 Version:         1.12.6
 API version:     1.24
 Package version: docker-1.12.6-71.git3e8e77d.el7.centos.1.x86_64
 Go version:      go1.8.3
 Git commit:      3e8e77d/1.12.6
 Built:           Tue Jan 30 09:17:00 2018
 OS/Arch:         linux/amd64
[root@k8s-master ~]# 
[root@k8s-master ~]# kubectl get version
the server doesn't have a resource type "version"
[root@k8s-master ~]# kubectl version
Client Version: version.Info{Major:"1", Minor:"9", GitVersion:"v1.9.0", GitCommit:"925c127ec6b946659ad0fd596fa959be43f0cc05", GitTreeState:"clean", BuildDate:"2017-12-15T21:07:38Z", GoVersion:"go1.9.2", Compiler:"gc", Platform:"linux/amd64"}
Server Version: version.Info{Major:"1", Minor:"9", GitVersion:"v1.9.3", GitCommit:"d2835416544f298c919e2ead3be3d0864b52323b", GitTreeState:"clean", BuildDate:"2018-02-07T11:55:20Z", GoVersion:"go1.9.2", Compiler:"gc", Platform:"linux/amd64"}

```

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
63950574b491        huanwei/rocketmq:4.3.0   "sh mqbroker"       9 minutes ago       Up 9 minutes        0.0.0.0:10909->10909/tcp, 9876/tcp, 0.0.0.0:10911->10911/tcp   rmqbroker
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