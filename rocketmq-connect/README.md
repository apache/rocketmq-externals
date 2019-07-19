# rocketmq-connector 

# 快速开始

文档以rocketmq-connect-file作为demo

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

# Local file dir for config store
storePathRootDir=～/storeRoot

#需要修改为自己的rocketmq
# Rocketmq namesrvAddr
namesrvAddr=127.0.0.1:9876  

#需要修改，修改为rocketmq-connect-file target目录加载demo中source/sink
# Source or sink connector jar file dir
pluginPaths=/home/connect/file-connect/target
``` 

## 4.运行

返回recoketmq-connect-runtime根目录运行
```
sh ./run_worker.sh
```

查看日志文件${user.home}/logs/rocketmqconnect/connect_runtime.log
以下日志表示runtime启动成功：

The worker [DEFAULT_WORKER_1] boot success.

```
注：启动之前RocketMQ创建以下topic
cluster-topic 集群信息
config-topic  配置信息
offset-topic  sink消费进度
position-topic source数据处理进度
并且为了保证消息有序，每个topic可以只建一个queue
```

## 5.日志目录

 ${user.home}/logs/rocketmqconnect 

## 6.配置文件

持久化配置文件默认目录 ～/storeRoot

1. connectorConfig.json connector配置持久化文件
2. position.json        source connect数据处理进度持久化文件
3. taskConfig.json      task配置持久化文件
4. offset.json          sink connect数据消费进度持久化文件

## 7.启动source connector

```
    GET请求  
    http://(your worker ip):(port)/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector","topic":"fileTopic","filename":"/root/connect/rocketmq-externals/rocketmq-connect-runtime/source-file.txt","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}   
```
   看到一下日志说明file source connector启动成功了
   
   2019-07-16 11:18:39 INFO pool-7-thread-1 - Source task start, config:{"properties":{"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter","filename":"/root/connect/rocketmq-externals/rocketmq-connect-runtime/source-file.txt","task-class":"org.apache.rocketmq.connect.file.FileSourceTask","topic":"fileTopic","connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector","update-timestamp":"1563247119715"}}
```  
    注：创建topic："topic":"fileTopic"
```

#### source connector配置说明

|key               |nullable|default    |description|
|------------------|--------|-----------|-----------|
|connector-class         |false   |           |实现Connector接口的类名称（包含包名）|
|filename        |false   |           |数据源文件名称|
|task-class         |false   |           |实现SourceTask类名称（包含包名）|
|topic         |false   |           |同步文件数据所需topic|
|update-timestamp         |false   |           |配置更新时间戳|
|source-record-converter         |false   |           |Full class name of the impl of the converter used to convert SourceDataEntry to byte[]|


## 8.启动sink connector

```
    GET请求  
    http://(your worker ip):(port)/connectors/testSinkConnector1?config={"connector-class":"org.apache.rocketmq.connect.file.FileSinkConnector","topicNames":"fileTopic","filename":"/root/connect/rocketmq-externals/rocketmq-connect-runtime/sink-file.txt","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```  
看到一下日志说明file sink connector启动成功了
2019-07-16 11:24:58 INFO pool-7-thread-2 - Sink task start, config:{"properties":{"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter","filename":"/root/connect/rocketmq-externals/rocketmq-connect-runtime/sink-file.txt","topicNames":"fileTopic","task-class":"org.apache.rocketmq.connect.file.FileSinkTask","connector-class":"org.apache.rocketmq.connect.file.FileSinkConnector","update-timestamp":"1563247498694"}}

查看配置中"filename":"/root/connect/rocketmq-externals/rocketmq-connect-runtime/sink-file.txt"配置文件
如果sink-file.txt生成并且与source-file.txt内容一样，说明整个流程已经跑通

#### sink connector配置说明

|key               |nullable|default    |description|
|------------------|--------|-----------|-----------|
|connector-class         |false   |           |实现Connector接口的类名称（包含包名）|
|topicNames        |false   |           |sink需要处理数据消息topics|
|task-class         |false   |           |实现SourceTask类名称（包含包名）|
|filename         |false   |           |sink拉去的数据保存到文件|
|update-timestamp         |false   |           |配置更新时间戳|
|source-record-converter         |false   |           |Full class name of the impl of the converter used to convert SourceDataEntry to byte[]|

```  
注：source/sink配置文件说明是以rocketmq-connect-file为demo，不同source/sink connector配置有差异，请以具体sourc/sink connector为准
```  

##FAQ
Q1：sink-file.txt文件中每行的文本顺序source-file.txt不一致？

A1: source数据到sink中经过rocketmq中转，如果需要顺序消息，需要有序消息发送到同一个queue。
实现有序消息有2中方式：1、一个topic创建一个queue（rocketmq-connect-runtime目前只能使用这种方式）2、rocketmq-connect-runtime支持顺序消息，通过消息中指定字段处理发送到rocketmq的同一queue（后续支持）





