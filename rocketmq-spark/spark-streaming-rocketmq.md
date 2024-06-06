# RocketMQ + Spark Streaming Integration Guide

This project is used to receive message from RocketMQ for Spark Streaming. Both push & pull consumer mode are provided. It provides simple parallelism, 1:1 correspondence between RocketMq's message queue id and Spark partitions.

## Install
For Scala/Java applications using SBT/Maven project definitions, link your streaming application with the following artifact.

	groupId = org.apache.rocketmq
	artifactId = rocketmq-spark
	version = 0.0.1-SNAPSHOT
	
In fact we may not find the artifact, So we should execute the following command in rocketmq-spark root directory firstly.

 `mvn clean install dependency:copy-dependencies`
 
## Creating a RocketMq Stream (Using Consumer Pull Mode)

There are two approaches to configure spark streaming to read date from RocketMq.
1. Approach 1 use `RocketMqUtils.createMQPullStream(...)` to create direct stream which ensure stronger end-to-end guarantees.
2. Approach 2 use `RocketMQUtils.createInputDStream(...)` to create receiver stream which is more complex than direct stream.

Firstly, the approach 1 is introduced. 

For Scala:

```
  val dStream: InputDStream[MessageExt] = RocketMqUtils.createMQPullStream(streamingContext, groupId, topic, ConsumerStrategy.earliest, true, false, false)

  dStream.map(message => (message.getBody)).print()
```

For Java:

```
    JavaInputDStream<MessageExt> stream = RocketMqUtils.createJavaMQPullStream(javaStreamingContext, groupId,
                topics, ConsumerStrategy.earliest(), true, false, false);
        
        stream.foreachRDD(new VoidFunction<JavaRDD<MessageExt>>() {
            @Override
            public void call(JavaRDD<MessageExt> messageExtJavaRDD) throws Exception {
                messageExtJavaRDD.foreach(new VoidFunction<MessageExt>() {
                    @Override
                    public void call(MessageExt messageExt) throws Exception {
                        System.out.println(messageExt.toString());
                    }
                });
            }
        });
```

## Creating a RocketMq RDD

For Scala:

```
    val offsetRanges = new util.HashMap[TopicQueueId, Array[OffsetRange]]
    val topicQueueId1 = new TopicQueueId("topic", 1)
    val ranges1 = Array(OffsetRange("groupId", 1, "broker-1", 0, 100), OffsetRange("groupId", 1, "broker-2", 0, 100))
    offsetRanges.put(topicQueueId1, ranges1)
    val topicQueueId2 = new TopicQueueId("topic", 2)
    val ranges2 = Array(OffsetRange("groupId", 2, "broker-1", 0, 100), OffsetRange("groupId", 2, "broker-2", 0, 100))
    offsetRanges.put(topicQueueId1, ranges2)
    val optionParams = new util.HashMap[String, String]
    
    val rdd: RDD[MessageExt] = RocketMqUtils.createRDD(sparkContext, groupId, offsetRanges, optionParams)
    rdd.foreach(message => System.out.println(message.getBody))
```

For Java:

```
    ap<TopicQueueId,  OffsetRange[]> offsetRanges = new HashMap<>();
    TopicQueueId topicQueueId1 = new TopicQueueId("topic", 1);
    OffsetRange [] ranges1 = {OffsetRange.create("groupId", 1, "broker-1", 0, 100),
            OffsetRange.create("groupId", 1, "broker-2", 0, 100)};
    offsetRanges.put(topicQueueId1, ranges1);

    TopicQueueId topicQueueId2 = new TopicQueueId("topic", 2);
    OffsetRange [] ranges2 = {OffsetRange.create("groupId", 2, "broker-1", 0, 100),
            OffsetRange.create("groupId", 2, "broker-2", 0, 100)};
    offsetRanges.put(topicQueueId2, ranges2);

    Map<String, String>  optionParams= new HashMap();
    LocationStrategy  locationStrategy = LocationStrategies.PreferConsistent();

    JavaRDD<MessageExt> rdd = RocketMqUtils.createJavaRDD(sparkContext, groupId, offsetRanges,
            optionParams, locationStrategy);
    
    rdd.foreach(new VoidFunction<MessageExt>() {
        @Override
        public void call(MessageExt messageExt) throws Exception {
            System.out.println(messageExt.getBodyCRC());
        }
    });
```

## LocationStrategies

The RocketMq consumer API will pre-fetch messages into buffers.  Therefore it is important for performance reasons that the Spark integration keep cached consumers on executors (rather than recreating them for each batch), and prefer to schedule partitions on the host locations that have the appropriate consumers.

In most cases, you should use `LocationStrategies.PreferConsistent` as shown above.  This will distribute partitions evenly across available executors.   Finally, if you have a significant skew in load among partitions, use `PreferFixed`. This allows you to specify an explicit mapping of partitions to hosts (any unspecified partitions will use a consistent location).

The cache for consumers has a default maximum size of 64.  If you expect to be handling more than (64 * number of executors) RocketMq partitions, you can change this setting via `pull.consumer.cache.maxCapacity`

## ConsumerStrategy

The RocketMq consumer will start to consume from different offset based on different consumer strategy.

1. EarliestStrategy:  Specify the earliest available offset for every message queue to start to consume. But if the Rocketmq server has checkpoint for a message queue, then the consumer will consume from the checkpoint.

2. LatestStrategy: Specify the lastest available offset for every message queue to start to consume. But if the Rocketmq server has checkpoint for a message queue, then the consumer will consume from the checkpoint.

3. SpecificOffsetStrategy:  Specify the specific available offset for every message queue to start to consume. Generally if the Rocketmq server has checkpoint for a message queue, then the consumer will consume from the checkpoint. But if the forceSpecial is true, the consumer will start to consume from the specific available offset in any case. Of course, the consumer will start to consume from the min available offset if a message queue is not specified. If the specify offset is 'ConsumerStrategy.LATEST' for a message queue, it indicates resolution to the latest offset. And if the specify offset is 'ConsumerStrategy.EARLIEST', it indicates resolution to the earliest offset.

## Obtaining Offsets

Note that the typecast to HasOffsetRanges will only succeed if it is done in the first method called on the result of createMQPullStream, not later down a chain of methods. You can use transform() instead of foreachRDD() as your first method call in order to access offsets, then call further Spark methods. Be aware that the one-to-one mapping between RDD partition and RocketMq partition does not remain after any methods that shuffle or repartition, e.g. reduceByKey() or window().

`dStream.foreachRDD { rdd =>
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd.foreachPartition { iter =>
          val queueId = TaskContext.get.partitionId
          val offsets: Array[OffsetRange] = offsetRanges.get(new TopicQueueId(topic, queueId))
      }
    }`
    
    
## Storing Offsets

RocketMq delivery semantics in the case of failure depend on how and when offsets are stored. Spark output operations are at-least-once. So if you want the equivalent of exactly-once semantics, you must either store offsets after an idempotent output, or store offsets in an atomic transaction alongside output. With this integration, you have 3 options, in order of increasing reliability (and code complexity), for how to store offsets.

### Checkpoints

If you enable Spark checkpointing, offsets will be stored in the checkpoint. This is easy to enable, but there are drawbacks. Your output operation must be idempotent, since you will get repeated outputs; transactions are not an option. Furthermore, you cannot recover from a checkpoint if your application code has changed. For planned upgrades, you can mitigate this by running the new code at the same time as the old code (since outputs need to be idempotent anyway, they should not clash). But for unplanned failures that require code changes, you will lose data unless you have another way to identify known good starting offsets.

### Storing offsets based on RocketMq Server

RocketMq has an offset commit API that stores offsets in a special RocketMq topic. By default, the new consumer will auto-commit offsets by setting "autoCommit" true. This is almost certainly not what you want, because messages successfully polled by the consumer may not yet have resulted in a Spark output operation, resulting in undefined semantics. Then messages maybe lost. However, you can commit offsets to Rocket after you  your output has been stored, using the commitAsync API.At the same time, you must make "autoCommit" be false. The benefit as compared to checkpoints is that RocketMq is a durable store regardless of changes to your application code. However, RocketMq is not transactional, so your outputs must still be idempotent.

For Scala:

```
    //store commits
    dStream.foreachRDD { rdd =>
        val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        // some time later, after outputs have completed
        dStream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    }
```

For Java:

```
    dStream.foreachRDD(rdd -> {
        OffsetRange[] offsetRanges = ((HasOffsetRanges) rdd.rdd()).offsetRanges();
        // some time later, after outputs have completed
        ((CanCommitOffsets) stream.inputDStream()).commitAsync(offsetRanges);
    });
```


### Commit offsets based on your own data store

For data stores that support transactions, saving offsets in the same transaction as the results can keep the two in sync, even in failure situations.  If you're careful about detecting repeated or skipped offset ranges, rolling back the transaction prevents duplicated or lost messages from affecting results.  This gives the equivalent of exactly-once semantics.  It is also possible to use this tactic even for outputs that result from aggregations, which are typically hard to make idempotent.

```
    // begin from the the offsets committed to the database
    val fromOffsets = selectOffsetsFromYourDatabase.map { resultSet =>
      new MessageQueue(resultSet.string("topic"), resultSet.string("broker"),
        resultSet.int("queueId")) -> resultSet.long("offset")
    }.toMap
    
    val specificStrategy = ConsumerStrategy.specificOffset(fromOffsets)
    val stream = RocketMqUtils
      .createMQPullStream(streamingContext, groupId, topic, specificStrategy, false, true, true)

    stream.foreachRDD { rdd =>
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

      val results = yourCalculation(rdd)

      // begin your transaction

      // update results
      // update offsets where the end of existing offsets matches the beginning of this batch of offsets
      // assert that offsets were updated correctly

      // end your transaction
    }
```

## RocketMQConfig

_The following configs are for Consumer Pull Mode_

|Property Name | Default | Meaning |
| ------------ | --------| ------ |
| pull.max.speed.per.partition | -1 | Maximum rate (number of records per second) at which data will be read from each RocketMq partition, and the default value is "-1", it means consumer can pull message from rocketmq as fast as the consumer can. Other that, you also enables or disables Spark Streaming's internal backpressure mechanism by the config "spark.streaming.backpressure.enabled". |
|pull.max.batch.size|32|To pick up the consume speed, the consumer can pull a batch of messages at a time.|
|pull.timeout.ms|3000|pull timeout for the RocketMq consumer|
|pull.consumer.cache.initialCapacity| 16|the configs for consumer cache|
|pull.consumer.cache.maxCapacity| 64|the configs for consumer cache|
|pull.consumer.cache.loadFactor|0.75|the configs for consumer cache|
|mq.pull.consumer.provider.factory.name|DefaultSimpleFactory| Custom pull consumer instance can be set by this config|

## failOnDataLoss

Whether to fail the query when it's possible that data is lost (e.g., topics are deleted, or offsets are out of range). This may be a false alarm. You can disable it when it doesn't work as you expected.

## Custom MQ Pull Consumer

The pull consumer factory is implemented in java SPI(Service Provider Interface) method. The default factory is `SimpleMQPullConsumerProviderFactory` which is configured by `'mq.pull.consumer.provider.factory.name' = 'DefaultSimpleFactory'`. If you want to customize pull consumer, the three steps can be followed.
1. Step1: Implement `MQPullConsumerProviderFactory` and `MQPullConsumerProvider`.
2. Step2: Put the custom `MQPullConsumerProviderFactory` fully qualified domain name to `services` file named `org.apache.rocketmq.spark.streaming.MQPullConsumerProviderFactory`.
3. Step3: Set the configuration `MQ_PULL_CONSUMER_PROVIDER_FACTORY_NAME` into option params when called method `RocketMqUtils.createJavaMQPullStream(...)` and the option value is the return value `getName()` of the custom `MQPullConsumerProviderFactory`.

## Approach 2: RocketMQ Receiver (Using Consumer Push Mode)

* RocketMQReceiver - which is no fault-tolerance guarantees
* ReliableRocketMQReceiver - which is fault-tolerance guarantees

### example:
```
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount");
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(1));
        Properties properties = new Properties();
        properties.setProperty(RocketMQConfig.NAME_SERVER_ADDR, NAMESERVER_ADDR);
        properties.setProperty(RocketMQConfig.CONSUMER_GROUP, CONSUMER_GROUP);
        properties.setProperty(RocketMQConfig.CONSUMER_TOPIC, CONSUMER_TOPIC);

        // no fault-tolerance guarantees
        JavaInputDStream ds = RocketMQUtils.createInputDStream(jssc, properties, StorageLevel.MEMORY_ONLY());
        // fault-tolerance guarantees
        // JavaInputDStream ds = RocketMQUtils.createReliableInputDStream(jssc, properties, StorageLevel.MEMORY_ONLY());
        ds.print();
        jssc.start();
        jssc.awaitTerminationOrTimeout(60000);
```

## Approach 1 (Direct Stream) VS Approach 2 (Receiver Stream)

### Direct Stream

This approach do not need to launch receivers in spark executors. The spark driver periodically fetch RocketMq for the latest offsets of each MessageQueues in specified topic. And then defines the offset ranges of batch job. When the batch job are launched the defined offset ranges are read from RocketMq and the data are processed.

### Receiver Stream

This approach uses a Receiver to receive data. The receivers are launched in spark executors and the data received from RocketMq through a receiver is stored in executors, and then spark streaming launched jobs to process the data.

Under default configuration, this approach can not guarantee zero-data loss under failures. If you do not want to lose data you can enable write-ahead logs in spark streaming. This saves all received data into write-ahead logs on a distributed file system, e.g. HDFS. In this way, all data can be recovered from distribute storage on failure.