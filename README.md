rocketmq-flume Source&Sink
==========================

该项目用于[RocketMQ](https://github.com/alibaba/RocketMQ)与[Flume-ng](https://github.com/apache/flume)之间的消息接收和投递。

1. 首先请确定您已经对[RocketMQ](https://github.com/alibaba/RocketMQ)和[Flume-ng](https://github.com/apache/flume)有了基本的了解
2. 确保本地maven库中已经存在[RocketMQ相关的包](https://github.com/alibaba/RocketMQ/releases/download/v3.2.2/alibaba-rocketmq-client-java-3.2.2.tar.gz)，或者下载RocketMQ源码自行编译
3. 在rocketmq-flume项目根目录执行`mvn clean install dependency:copy-dependencies`
4. 将rocketmq-flume相关依赖jar包拷贝到`$FLUME_HOME/lib`目录中(具体包会在后面描述)

## Sink

### Sink配置说明

| 配置项         | 必填 | 默认值            | 说明 |
|---------------|-----|------------------|------|
| namesrvAddr   | 必填 | null             | Name Server地址，遵循RocketMQ配置方式 |
| producerGroup | 可选 | DEFAULT_PRODUCER | Producer分组 |
| topic         | 必填 | null             | Topic名称 |
| tags          | 可选 | 空字符串          | Tag名称，遵循RocketMQ配置方式 |

### Sink综合示例

- 编写Flume的配置文件

```
agent1.sources=source1
agent1.channels=channel1
agent1.sinks=sink1

agent1.sources.source1.type=avro
agent1.sources.source1.bind=0.0.0.0
agent1.sources.source1.port=15151
agent1.sources.source1.channels=channel1

agent1.sinks.sink1.type=com.handu.flume.sink.rocketmq.RocketMQSink
agent1.sinks.sink1.namesrvAddr=rocketmq_namesrv:9876
agent1.sinks.sink1.producerGroup=MyProducerGroup_1
agent1.sinks.sink1.topic=FromFlume
agent1.sinks.sink1.tag=Tag1
agent1.sinks.sink1.channel=channel1

agent1.channels.channel1.type=memory
agent1.channels.channel1.capacity=100
agent1.channels.channel1.transactionCapacity=100
agent1.channels.channel1.keep-alive=3
```

- 将下面jar包拷贝到`$FLUME_HOME/lib`目录中

```
rocketmq-flume-sink-1.0.0.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target)
fastjson-1.1.41.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.23.Final.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- 执行测试命令查看

```
shell1> $FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
shell2> $FLUME_HOME/bin/flume-ng avro-client -H localhost -p 15151 -F $FLUME_HOME/README
```

- 查看shell1控制台输出的信息

## Source

### Source配置说明

| 配置项         | 必填 | 默认值            | 说明 |
|---------------|-----|------------------|------|
| namesrvAddr   | 必填 | null             | Name Server地址，遵循RocketMQ配置方式 |
| consumerGroup | 可选 | DEFAULT_CONSUMER | Consumer分组 |
| topic         | 必填 | null             | Topic名称 |
| tags          | 可选 | *                | Tag名称，遵循RocketMQ配置方式 |
| messageModel  | 可选 | BROADCASTING     | BROADCASTING或CLUSTERING |
| maxNums       | 可选 | 32               | 一次读取消息数量 |

### Source综合示例

```
agent1.sources=source1
agent1.channels=channel1
agent1.sinks=sink1

agent1.sources.source1.type=com.handu.flume.source.rocketmq.RocketMQSource
agent1.sources.source1.namesrvAddr=rocketmq_namesrv:9876
agent1.sources.source1.consumerGroup=MyConsumerGroup_1
agent1.sources.source1.topic=TopicTest
agent1.sources.source1.tags=*
agent1.sources.source1.messageModel=BROADCASTING
agent1.sources.source1.maxNums=32
agent1.sources.source1.channels=channel1

agent1.sinks.sink1.type=logger
agent1.sinks.sink1.channel=channel1

agent1.channels.channel1.type=memory
agent1.channels.channel1.capacity=100
agent1.channels.channel1.transactionCapacity=100
agent1.channels.channel1.keep-alive=3
```

- 将下面jar包拷贝到`$FLUME_HOME/lib`目录中

```
rocketmq-flume-source-1.0.0.jar (文件位置: $PROJECT_HOME/rocketmq-flume-source/target)
fastjson-1.1.41.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.23.Final.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-3.2.2.jar (文件位置: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- 向RocketMQ中投递一些测试消息

- 执行测试命令查看控制台输出

```
$FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
```
