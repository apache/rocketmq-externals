---
description: 快速构建并使用RocketMQ Runtime
---

# RocketMQ Runtime

## RocketMQ Runtime 快速开始

### 环境依赖

1. **64bit JDK 1.8+;**
2. **Maven 3.2.x或以上版本;**
3. **RocketMQ集群环境;**

### **项目下载**

```bash
$ git clone https://github.com/apache/rocketmq-externals
```

### 项目构建

```bash
$ cd rocketmq-externals/rocketmq-connect
$ mvn clean install -Dmaven.test.skip=true
```

### 项目配置

#### 修改配置文件connect.conf

```bash
$ cd rocketmq-connect/rocketmq-connect-runtime/target/distribution/conf
$ vim  connect.conf
```

```typescript
# 当前的节点的独特Id
workerId=DEFAULT_WORKER_1

# REST API的端口地址
httpPort=8081

# 本地存储路径
storePathRootDir=～/storeRoot

# 需要修改为自己的rocketmq NameServer的端口地址
# Rocketmq namesrvAddr
namesrvAddr=127.0.0.1:9876  

#需要修改为connector-plugins文件夹所在的位置
# Source or sink connector jar file dir
pluginPaths=/usr/local/connector-plugins/

```

### 创建Topic

* 进入到rocketmq的包中

```bash
# 进入到rocketmq中的目录中
$ cd {$rockemq-目录}/rocketmq
# 进入到mqadmin所在的目录
$ cd bin

```

* 使用`mqadmin`在broker集群上创建对应的Topic

```bash
$ sh mqadmin
# 通过上面的命令我们可以看到很多常用命令
The most commonly used mqadmin commands are:
   updateTopic          Update or create topic
   deleteTopic          Delete topic from broker and NameServer.
   updateSubGroup       Update or create subscription group
   deleteSubGroup       Delete subscription group from broker.
   ...
   ...
See 'mqadmin help <command>' for more information on a specific command.

# 同样可以使用 'mqadmin helo <command>' 获取命令操作
$ sh mqadmin help upadteTopic
usage: mqadmin updateTopic -b <arg> | -c <arg>  [-h] [-n <arg>] [-o <arg>] [-p <arg>] [-r <arg>] [-s <arg>] -t
       <arg> [-u <arg>] [-w <arg>]
 -b,--brokerAddr <arg>       create topic to which broker
 -c,--clusterName <arg>      create topic to which cluster
 -h,--help                   Print help
 -n,--namesrvAddr <arg>      Name server address list, eg: 192.168.0.1:9876;192.168.0.2:9876
 -o,--order <arg>            set topic's order(true|false)
 -p,--perm <arg>             set topic's permission(2|4|6), intro[2:W 4:R; 6:RW]
 -r,--readQueueNums <arg>    set read queue nums
 -s,--hasUnitSub <arg>       has unit sub (true|false)
 -t,--topic <arg>            topic name
 -u,--unit <arg>             is unit topic (true|false)
 -w,--writeQueueNums <arg>   set write queue nums
```

* 可以看到 `updateTopic` 的参数设置,并用此创建

{% hint style="success" %}
这里 -b 需要broker的地址，需要换成自己的集群地址
{% endhint %}

```bash
# 创建集群发现的Topic
$ sh mqadmin updateTopic -b 4xx.1xx.2xx.1xx:10911 -t connector-cluster-topic
....
create topic to 4xx.1xx.2xx.1xx:10911 success.
TopicConfig [topicName=connector-cluster-topic, readQueueNums=8, writeQueueNums=8, perm=RW-, topicFilterType=SINGLE_TAG, topicSysFlag=0, order=false] 

# 创建connector设置信息Topic
$ sh mqadmin updateTopic -b 47.106.2xx.1xx:10911 -t connector-config-topic
....
create topic to 4xx.1xx.2xx.1xx:10911 success.
TopicConfig [topicName=connector-config-topic, readQueueNums=8, writeQueueNums=8, perm=RW-, topicFilterType=SINGLE_TAG, topicSysFlag=0, order=false]

# 创建Source Connector消费Topic
$ sh mqadmin updateTopic -b 4xx.1xx.2xx.1xx:10911 -t connector-position-topic
....
create topic to 4xx.1xx.2xx.1xx:10911 success.
TopicConfig [topicName=connector-position-topic, readQueueNums=8, writeQueueNums=8, perm=RW-, topicFilterType=SINGLE_TAG, topicSysFlag=0, order=false]

# 创建Sink Connector消费Topic
$ sh mqadmin updateTopic -b 4xx.1xx.2xx.1xx:10911 -t connector-offset-topic
....
create topic to 4xx.1xx.2xx.1xx:10911 success.
TopicConfig [topicName=connector-offset-topic, readQueueNums=8, writeQueueNums=8, perm=RW-, topicFilterType=SINGLE_TAG, topicSysFlag=0, order=false]

```

更多配置参数，请参考文档

#### [RocketMQ Runtime配置文档](../rocketmq-connect-1/rocketmq-runtime/runtime-can-shu-pei-zhi.md)

### 项目运行

{% hint style="info" %}
#### 运行前请确认

1. Nameserv 已经启动
2. Broker 已启动
{% endhint %}

#### 运行脚本

```bash
# 返回到rocketmq-runtime目录
$ cd ../../../

# 运行脚本
$ sh ./run_worker.sh

run rumtime worker
2019-10-11 10:46:57 INFO main - Logging initialized @1207ms to org.eclipse.jetty.util.log.Slf4jLog
2019-10-11 10:46:57 INFO main - 
 _________________________________________
|        _                  _ _           |
|       | | __ ___   ____ _| (_)_ __      |
|    _  | |/ _` \ \ / / _` | | | '_ \     |
|   | |_| | (_| |\ V / (_| | | | | | |    |
|    \___/ \__,_| \_/ \__,_|_|_|_| |_|    |
|_________________________________________|
|                                         |
|    https://javalin.io/documentation     |
|_________________________________________|
...

The worker [10.134.142.157@43326] boot success.
```

**看到如下信息表示成功**

```bash
The worker [10.134.142.157@43326] boot success.
```

#### 查看日志

查看日志文件

```bash
$ cd ~/logs/rocketmqconnect/
```

可以看到对应的日志

