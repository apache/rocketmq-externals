rocketmq-flume Source&Sink
==========================

This project is used to receive and send messages between
[RocketMQ](https://github.com/alibaba/RocketMQ) and [Flume-ng](https://github.com/apache/flume)

1. Firstly,please confirm you have known about [RocketMQ](https://github.com/alibaba/RocketMQ) and [Flume-ng](https://github.com/apache/flume).
2. Ensure that the jar related to [RocketMQ](https://github.com/alibaba/RocketMQ/releases/download/v3.2.2/alibaba-rocketmq-client-java-3.2.2.tar.gz) exists in local maven repository.
3. Execute the command in rocketmq-flume root directory `mvn clean install dependency:copy-dependencies`
4. Copy the jar depended by rocketmq-flume to `$FLUME_HOME/lib`(the specific jar will be given later)

## Sink

### Sink configuration instruction

| key           |        | default            |
|---------------|--------|------------------|
| namesrvAddr   | required | null             |
| producerGroup | optional | "DEFAULT_PRODUCER" |
| topic         | required | null             |
| tags          | optional | ""         |

### Sink example

- Write the Flume configuration file

```
agent1.sources=source1
agent1.channels=channel1
agent1.sinks=sink1

agent1.sources.source1.type=avro
agent1.sources.source1.bind=0.0.0.0
agent1.sources.source1.port=15151
agent1.sources.source1.channels=channel1

agent1.sinks.sink1.type=org.apache.rocketmq.flume.ng.sink.RocketMQSink
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

- Copy the jars below to `$FLUME_HOME/lib`

```
rocketmq-flume-sink-1.0.0.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target)
fastjson-1.1.41.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.23.Final.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- Execute the command and check the console output

```
shell1> $FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
shell2> $FLUME_HOME/bin/flume-ng avro-client -H localhost -p 15151 -F $FLUME_HOME/README
```


## Source

### Source configuration instruction

| key         |  | default            |
|---------------|-----|------------------|
| namesrvAddr   | required | null             |
| consumerGroup | optional | "DEFAULT_CONSUMER" |
| topic         | required | null             |
| tags          | optional | *                |
| messageModel  | optional | "BROADCASTING"     |
| maxNums       | optional | 32               |

### Source example
- Write the Flume configuration file

```
agent1.sources=source1
agent1.channels=channel1
agent1.sinks=sink1

agent1.sources.source1.type=org.apache.rocketmq.flume.ng.source.RocketMQSource
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

- Copy the jars below to `$FLUME_HOME/lib`

```
rocketmq-flume-source-1.0.0.jar (path: $PROJECT_HOME/rocketmq-flume-source/target)
fastjson-1.1.41.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.23.Final.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-3.2.2.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- Send test message to RocketMQ

- Execute the command and check the console output

```
$FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
```
