# RocketMQ + Spark Structured Streaming Integration Guide

Structured Streaming integration for RocketMQ to read data from and write data to RocketMQ.

## Reading Data from RocketMQ

### Creating a RocketMQ Source for Streaming Queries

```
// Subscribe to a topic
Dataset<Row> df = spark
    .readStream()
    .format("org.apache.spark.sql.rocketmq.RocketMQSourceProvider")
    .option("nameServer", "host:port")
    .option("topic", "myTopic")
    .load()
df.selectExpr("CAST(body AS STRING)")
```

### Creating a RocketMQ Source for Batch Queries

If you have a use case that is better suited to batch processing, you can create a Dataset/DataFrame for a defined range of offsets.

```
Dataset<Row> dfInput = spark
    .read()
    .format("org.apache.spark.sql.rocketmq.RocketMQSourceProvider")
    .option("nameServer", "host:port")
    .option("topic", "myTopic")
    .load();
df.selectExpr("CAST(body AS STRING)")
```

Each row in the source has the following schema:

| Column         | Type      |
|----------------|-----------|
| topic          | String    |
| flag           | Integer   |
| body           | Binary    |
| properties     | String    |
| brokerName     | String    |
| queueId        | Integer   |
| queueOffset    | Long      |
| bornTimestamp  | Timestamp |
| storeTimestamp | Timestamp |

The following options MUST be set for the RocketMQ source for both batch and streaming queries.

| Option     | Value                 | Meaning                         |
|------------|-----------------------|---------------------------------|
| nameServer | a string as host:port | NameServer address for RocketMQ |
| topic      | a string              | Subscribed topic                |

The following configurations are optional:

| Option | Value  | Default | Query Type | Meaning |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| startingOffsets | "earliest", "latest" (streaming only), or json string {"topicA":{"broker1":{"0":23,"1":-1},"broker2":{"0":23}},"topicB":{"broker3":{"0":-2}}} | "latest" for streaming, "earliest" for batch | streaming and batch | The start point when a query is started, either "earliest" which is from the earliest offsets, "latest" which is just from the latest offsets, or a json string specifying a starting offset for each MessageQueue. In the json, -2 as an offset can be used to refer to earliest, -1 to latest. Note: For batch queries, latest (either implicitly or by using -1 in json) is not allowed. For streaming queries, this only applies when a new query is started, and that resuming will always pick up from where the query left off. |
| endingOffsets | latest or json string {"topicA":{"broker1":{"0":23,"1":-1},"broker2":{"0":23}},"topicB":{"broker3":{"0":-2}}} | "latest" | batch query | The end point when a batch query is ended, either "latest" which is just referred to the latest, or a json string specifying an ending offset for each MessageQueue. In the json, -1 as an offset can be used to refer to latest, and -2 (earliest) as an offset is not allowed. |
| failOnDataLoss | true or false | true | streaming query | Whether to fail the query when it's possible that data is lost (e.g., topics are deleted, or offsets are out of range). This may be a false alarm. You can disable it when it doesn't work as you expected. Batch queries will always fail if it fails to read any data from the provided offsets due to lost data. |
| subExpression | string | "*" | streaming and batch | Subscription expression |
| pullTimeoutMs | long | 3000 | streaming and batch | The timeout in milliseconds to pull data from RocketMQ in executors. |
| pullBatchSize | int | 32 | streaming and batch | To pick up the consume speed, the consumer can pull a batch of messages at a time |

## Writing Data to RocketMQ

Here, we describe the support for writing Streaming Queries to RocketMQ. Take note that RocketMQ only supports at least once write semantics. Consequently, when writing—either Streaming Queries or Batch Queries—to RocketMQ, some records may be duplicated; this can happen, for example, if RocketMQ needs to retry a message that was not acknowledged by a Broker, even though that Broker received and wrote the message record. Structured Streaming cannot prevent such duplicates from occurring due to these write semantics. However, if writing the query is successful, then you can assume that the query output was written at least once. A possible solution to remove duplicates when reading the written data could be to introduce a primary (unique) key that can be used to perform de-duplication when reading.

### Creating a RocketMQ Sink for Streaming Queries

```
StreamingQuery query = dfOutput.writeStream()
    .format("org.apache.spark.sql.rocketmq.RocketMQSourceProvider")
    .option("nameServer", "host:port")
    .option("topic", "mySinkTopic")
    .start();
```

### Writing the output of Batch Queries to RocketMQ

```
df.write()
    .format("org.apache.spark.sql.rocketmq.RocketMQSourceProvider")
    .option("nameServer", "host:port")
    .option("topic", "mySinkTopic")
    .save();
```

The Dataframe being written to RocketMQ should have the following columns in schema:

| Column            | Type             |
|-------------------|------------------|
| body (required)   | string or binary |
| topic (*optional) | string           |
| tags (optional)   | string           |

*\* The topic column is required if the “topic” configuration option is not specified.*

The `body` column is the only required option. If a topic column exists then its value is used as the topic when writing the given row to RocketMQ, unless the “topic” configuration option is set i.e., the “topic” configuration option overrides the topic column.

The following options must be set for the RocketMQ sink for both batch and streaming queries.

| Option     | Value                 | Meaning                         |
|------------|-----------------------|---------------------------------|
| nameServer | a string as host:port | NameServer address for RocketMQ |

The following configurations are optional:

| Option | Value | Default | Query Type | Meaning |
|--------|--------|---------|---------------------|---------------------------------------------------------------------------------------------------------------------------------|
| topic | string | none | streaming and batch | Sets the topic that all rows will be written to in RocketMQ. This option overrides any topic column that may exist in the data. |
