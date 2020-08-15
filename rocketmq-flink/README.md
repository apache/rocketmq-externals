# RocketMQ-Flink

RocketMQ integration for [Apache Flink](https://flink.apache.org/). This module includes the RocketMQ source and sink that allows a flink job to either write messages into a topic or read from topics in a flink job.

## RocketMQSource
To use the `RocketMQSource`,  you construct an instance of it by specifying a KeyValueDeserializationSchema instance and a Properties instance which including rocketmq configs.
`RocketMQSource(KeyValueDeserializationSchema<OUT> schema, Properties props)`
The RocketMQSource is based on RocketMQ pull consumer mode, and provides exactly once reliability guarantees when checkpoints are enabled.
Otherwise, the source doesn't provide any reliability guarantees.

### KeyValueDeserializationSchema
The main API for deserializing topic and tags is the `org.apache.rocketmq.flink.common.serialization.KeyValueDeserializationSchema` interface.
`rocketmq-flink` includes general purpose `KeyValueDeserializationSchema` implementations called `SimpleKeyValueDeserializationSchema`.

```java
public interface KeyValueDeserializationSchema<T> extends ResultTypeQueryable<T>, Serializable {
    T deserializeKeyAndValue(byte[] key, byte[] value);
}
```

## RocketMQSink
To use the `RocketMQSink`,  you construct an instance of it by specifying KeyValueSerializationSchema & TopicSelector instances and a Properties instance which including rocketmq configs.
`RocketMQSink(KeyValueSerializationSchema<IN> schema, TopicSelector<IN> topicSelector, Properties props)`
The RocketMQSink provides at-least-once reliability guarantees when checkpoints are enabled and `withBatchFlushOnCheckpoint(true)` is set.
Otherwise, the sink reliability guarantees depends on rocketmq producer's retry policy, for this case, the messages sending way is sync by default,
but you can change it by invoking `withAsync(true)`. 

### KeyValueSerializationSchema
The main API for serializing topic and tags is the `org.apache.rocketmq.flink.common.serialization.KeyValueSerializationSchema` interface.
`rocketmq-flink` includes general purpose `KeyValueSerializationSchema` implementations called `SimpleKeyValueSerializationSchema`.

```java
public interface KeyValueSerializationSchema<T> extends Serializable {

    byte[] serializeKey(T tuple);

    byte[] serializeValue(T tuple);
}
```

### TopicSelector
The main API for selecting topic and tags is the `org.apache.rocketmq.flink.common.selector.TopicSelector` interface.
`rocketmq-flink` includes general purpose `TopicSelector` implementations called `DefaultTopicSelector` and `SimpleTopicSelector`.

```java
public interface TopicSelector<T> extends Serializable {

    String getTopic(T tuple);

    String getTag(T tuple);
}
```

## Examples
The following is an example which receive messages from RocketMQ brokers and send messages to broker after processing.

 ```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // enable checkpoint
        env.enableCheckpointing(3000);

        Properties consumerProps = new Properties();
        consumerProps.setProperty(RocketMqConfig.NAME_SERVER_ADDR, "localhost:9876");
        consumerProps.setProperty(RocketMqConfig.CONSUMER_GROUP, "c002");
        consumerProps.setProperty(RocketMqConfig.CONSUMER_TOPIC, "flink-source2");

        Properties producerProps = new Properties();
        producerProps.setProperty(RocketMqConfig.NAME_SERVER_ADDR, "localhost:9876");

        env.addSource(new RocketMQSource(new SimpleKeyValueDeserializationSchema("id", "address"), consumerProps))
            .name("rocketmq-source")
            .setParallelism(2)
            .process(new ProcessFunction<Map, Map>() {
                @Override
                public void processElement(Map in, Context ctx, Collector<Map> out) throws Exception {
                    HashMap result = new HashMap();
                    result.put("id", in.get("id"));
                    String[] arr = in.get("address").toString().split("\\s+");
                    result.put("province", arr[arr.length-1]);
                    out.collect(result);
                }
            })
            .name("upper-processor")
            .setParallelism(2)
            .addSink(new RocketMQSink(new SimpleKeyValueSerializationSchema("id", "province"),
                new DefaultTopicSelector("flink-sink2"), producerProps).withBatchFlushOnCheckpoint(true))
            .name("rocketmq-sink")
            .setParallelism(2);

        try {
            env.execute("rocketmq-flink-example");
        } catch (Exception e) {
            e.printStackTrace();
        }
 ```

## Configurations
The following configurations are all from the class `org.apache.rocketmq.flink.RocketMQConfig`.

### Producer Configurations
| NAME        | DESCRIPTION           | DEFAULT  |
| ------------- |:-------------:|:------:|
| nameserver.address      | name server address *Required* | null |
| nameserver.poll.interval      | name server poll topic info interval     |   30000 |
| brokerserver.heartbeat.interval | broker server heartbeat interval      |    30000 |
| producer.group | producer group      |    `UUID.randomUUID().toString()` |
| producer.retry.times | producer send messages retry times      |    3 |
| producer.timeout | producer send messages timeout      |    3000 |


### Consumer Configurations
| NAME        | DESCRIPTION           | DEFAULT  |
| ------------- |:-------------:|:------:|
| nameserver.address      | name server address *Required* | null |
| nameserver.poll.interval      | name server poll topic info interval     |   30000 |
| brokerserver.heartbeat.interval | broker server heartbeat interval      |    30000 |
| consumer.group | consumer group *Required*     |    null |
| consumer.topic | consumer topic *Required*       |    null |
| consumer.tag | consumer topic tag      |    * |
| consumer.offset.reset.to | what to do when there is no initial offset on the server      |   latest/earliest/timestamp |
| consumer.offset.from.timestamp | the timestamp when `consumer.offset.reset.to=timestamp` was set   |   `System.currentTimeMillis()` |
| consumer.offset.persist.interval | auto commit offset interval      |    5000 |
| consumer.pull.thread.pool.size | consumer pull thread pool size      |    20 |
| consumer.batch.size | consumer messages batch size      |    32 |
| consumer.delay.when.message.not.found | the delay time when messages were not found      |    10 |


## License

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
