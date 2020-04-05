---
description: Replicator快速开始
---

# Replicator快速开始

## RocketMQ Replicator Quick Start

### 环境依赖

1. **64bit JDK 1.8+;**
2. **Maven 3.2.x或以上版本;**
3. **Git**
4. **RocketMQ集群环境;**

### 项目下载

```bash
$ git clone https://github.com/apache/rocketmq-externals.git
```

### 项目构建

```bash
$ cd rocketmq-externals/rocketmq-replicator
$ mvn clean install -Prelease-all -DskipTest -U

### 出现如下信息，项目构建成功
 

...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14.432 s
[INFO] Finished at: 2019-10-11T14:08:41+08:00
[INFO] ------------------------------------------------------------------------
```

### 项目部署使用

打包成功后将`rocketmq-replicator-0.1.0-SNAPSHOT-jar-with-dependencies.jar`（fatjar）放到`runtime`配置的`pluginPaths`目录下

```bash
$ cd target
# 拷贝jar包到RocketMQ Runtime插件文件夹
$ cp rocketmq-replicator-0.1.0-SNAPSHOT-jar-with-dependencies.jar /usr/local/connectors-plugin/
```

{% hint style="warning" %}
**RocketMQ Runtime插件文件路径可配置，需要和Runtime参数中的插件路径配置相对应。**

注：[Runtime 插件路径配置](../../rocketmq-connect-1/rocketmq-runtime/runtime-can-shu-pei-zhi.md#runtime-pei-zhi-can-shu-shuo-ming)
{% endhint %}

### 启动



发送GET请求，启动rocketmq-replicator

* 请求模板

```http
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-replicator-name}
 ?config={
 "connector-class":"org.apache.rocketmq.replicator.RmqSourceReplicator",
 "source-rocketmq":"xxxx:9876",
 "target-rocketmq":"xxxxxxx:9876",
 "replicator-store-topic":"replicatorTopic",
 "taskDivideStrategy":"0",
 "white-list":"TopicTest,TopicTest2",
 target-cluster":"{RocketMQCluster-Target}",
"source-cluster":"{RocketMQCluster-Source}",
 "task-parallelism":"2",
 "source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
 }
```

* [参数说明文档](replicator-can-shu-pei-zhi.md)
* 启动实例

```http
http://localhost:8081/connectors/replicator-test?config={
"connector-class":"org.apache.rocketmq.replicator.RmqSourceReplicator",
"source-rocketmq":"xxx.xxx.xxx.xxx:9876",
"target-rocketmq":"xxx.xxx.xxx.xxx:9876",
"replicator-store-topic":"replicatorTopic",
"taskDivideStrategy":"0",
"white-list":"fileTopic",
"task-parallelism":"4",
"target-cluster":"RocketMQCluster-xxx",
"source-cluster":"RocketMQCluster-xxx",
"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter",
"topic.rename.format":"rename-${topic}"
}
```

{% hint style="info" %}
* 上述namesrv地址需要换成自己的地址
* 参数'replicator-store-topic' 的topic要先在runtime集群创建
* 集群名称要替换成对应的集群名称
{% endhint %}

### 启动前确认

{% hint style="info" %}
请确认启动参数中**replicator-store-topic**和**white-list**的topic已经在**Source集群**创建

[Topic创建文档](https://rocketmq-1.gitbook.io/rocketmq-connector/quick-start/rocketmq-runtime-shi-yong#chuang-jian-topic)
{% endhint %}

### 停止

发送GET请求，停止rocketmq-replicator

* 请求模板

```http
http://${runtime-ip}:${runtime-port}/connectors/${replicator-name}/stop
```

* 停止示例

```http
http://localhost:8081/connectors/replicator-test/stop
```

{% hint style="info" %}
更多参数设置说明与Connector操作请参考 

* [replicator配置参数说明](replicator-can-shu-pei-zhi.md)
* [RESTful 接口](../../rocketmq-connect-1/rocketmq-runtime/restful-jie-kou.md)
{% endhint %}

