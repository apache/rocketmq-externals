rocketmq-flume-ng Sink & Source
==========================

This project is used to receive and send messages between
[RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume)

1. Firstly, please get familiar with [RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume).
2. Ensure that the jar related to [RocketMQ](http://rocketmq.incubator.apache.org/dowloading/releases) exists in local maven repository.
3. Execute the following command in rocketmq-flume root directory

   `mvn clean install dependency:copy-dependencies`

4. Copy the jar depended by rocketmq-flume to `$FLUME_HOME/lib`(the specific jar will be given later)

## Sink

### Sink configuration instruction

| key           | nullable | default                |description|
|---------------|----------|------------------------|-----------|
| nameserver    | false    |                        |nameserver address|
| topic         | true     | "FLUME_TOPIC"          |topic name|
| tag           | true     | "FLUME_TAG"            |tag name|
| producerGroup | true     | "FLUME_PRODUCER_GROUP" |producerGroup name|
| batchSize     | true     | 1                      |max batch event taking num|
| maxProcessTime| true     | 1000                   |max batch event taking time,default is 1s|

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
agent1.sinks.sink1.nameserver=x.x.x.x:9876
agent1.sinks.sink1.channel=channel1

agent1.channels.channel1.type=memory
agent1.channels.channel1.capacity=100
agent1.channels.channel1.transactionCapacity=100
agent1.channels.channel1.keep-alive=3
```

- Copy the jars below to `$FLUME_HOME/lib`

```
rocketmq-flume-sink-0.0.2-SNAPSHOT.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target)
fastjson-1.2.12.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.36.Final.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- Execute the command and check the console output

```
shell1> $FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
shell2> $FLUME_HOME/bin/flume-ng avro-client -H localhost -p 15151 -F $FLUME_HOME/README
```


## Source

### Source configuration instruction


| key           | nullable | default              |description|
|---------------|----------|----------------------|-----------|
| nameserver    | false    |                      |nameserver address|
| topic         | true     |"FLUME_TOPIC"         |topic name|
| tag           | true     |"FLUME_TAG"           |tag name|
| consumerGroup | true     |"FLUME_CONSUMER_GROUP"|consumerGroup name|
| messageModel  | true     | "BROADCASTING"       |RocketMQ message model,"BROADCASTING" or "CLUSTERING"|
| batchSize     | true     | 32                   |batch consuming messages from RocketMq max num|


### Source example
- Write the Flume configuration file

```
agent1.sources=source1
agent1.channels=channel1
agent1.sinks=sink1

agent1.sources.source1.type=org.apache.rocketmq.flume.ng.source.RocketMQSource
agent1.sources.source1.nameserver=x.x.x.x:9876
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
rocketmq-flume-source-0.0.2-SNAPSHOT.jar (path: $PROJECT_HOME/rocketmq-flume-source/target)
fastjson-1.2.12.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
netty-all-4.0.36.Final.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-client-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-common-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
rocketmq-remoting-4.0.0-incubating.jar (path: $PROJECT_HOME/rocketmq-flume-sink/target/dependency)
```

- Send some test message to RocketMQ

- Execute the command and check the console output

```
$FLUME_HOME/bin/flume-ng agent -c conf -f conf/flume.conf -n agent1 -Dflume.root.logger=INFO,console
```
