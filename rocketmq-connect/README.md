# rocketmq-connector 

# 快速开始

## 1.准备

1. 64bit JDK 1.8+;

2. Maven 3.2.x或以上版本;

3. A running RocketMQ cluster;


## 2.构建

```
mvn clean install -Dmaven.test.skip=true

```

## 3.配置

cd rocketmq-connect/rocketmq-connect-runtime/target/distribution/conf

1. 修改配置文件connect.conf

```
#current cluster node uniquely identifies
workerId=DEFAULT_WORKER_1

# Http prot for user to access REST API
httpPort=8081

# local file dir for config store
storePathRootDir=～/storeRoot

# rocketmq namesrvAddr
namesrvAddr=127.0.0.1:9876  

# source or sink connector jar file dir
pluginPaths=/home/connect/file-connect/target
``` 

## 4.运行

返回recoketmq-connect-runtime根目录运行
```
sh ./run_worker.sh

查看日志文件${user.home}/logs/rocketmqconnect/connect_runtime.log
以下日志表示runtime启动成功：

The worker [DEFAULT_WORKER_1] boot success.
```

```
注：启动之前RocketMQ创建以下topic
cluster-topic 集群信息
config-topic  配置信息
offset-topic  sink消费进度
position-topic source数据处理进度
并且为了保证消息有序，每个topic可以只建一个queue
```

#### 启动脚本可选参数

参数|说明
---|---
-c | 参数配置文件路径

## 5.日志目录

 ${user.home}/logs/rocketmqconnect

## 6.持久化文件

默认目录 ～/storeRoot

1. connectorConfig.json connector配置持久化文件
2. position.json        source connect数据处理进度持久化文件
3. taskConfig.json      task配置持久化文件
4. offset.json          sink connect数据消费进度持久化文件




