RocketMQ_exporter
==============

RocketMQ exporter for Prometheus.

Table of Contents
-----------------
-	[Compatibility](#compatibility)
-   [Dependency](#dependency)
-   [Download](#download)
-   [Compile](#compile)
	-   [Build Binary](#build-binary)
	-   [Build Docker Image](#build-docker-image)
-   [Run](#run)
	-   [Run Binary](#run-binary)
	-   [Run Docker Image](#run-docker-image)
-   [Flags](#flags)
-   [Metrics](#metrics)
	-   [Brokers](#brokers)
	-   [Topics](#topics)
	-   [Consumer Groups](#consumer-groups)
-   [Contribute](#contribute)

Compatibility
-------------

Support [Apache RocketMQ](https://rocketmq.apache.org) version 4.3.2 (and later).

Dependency
----------

-	[Prometheus](https://prometheus.io)

Compile
-------

### Build Binary

```shell
mvn clean install
```

### Build Docker Image

```shell
mvn package -Dmaven.test.skip=true docker:build
```

Run
---

### Run Binary

```shell
java -jar rocketmq-exporter-0.0.1-SNAPSHOT.jar [--rocketmq.config.namesrvAddr="127.0.0.1:9876" ...]
```

### Run Docker Image

```
docker container run -itd --rm  -p 5557:5557  breezecoolyang/rocketmq-exporter [--rocketmq.config.namesrvAddr="127.0.0.1:9876" ...]
```

Flags
---

This image is configurable using different flags

|Flag name                           | Default            | Description                                        |
| -----------------------------------|--------------------|----------------------------------------------------|
| `rocketmq.config.namesrvAddr`      |  127.0.0.1:9876 |name server address  for  broker cluster            |
| `rocketmq.config.webTelemetryPath` | /metrics           |Path under which to expose metrics                  |
| `server.port`                      | 5557               |Address to listen on for web interface and telemetry|
| `rocketmq.config.rocketmqVersion`  | V4_3_2             |rocketmq broker version                             |

Metrics
-------

Documents about exposed Prometheus metrics.

### Broker 

**Metrics details**

| Name         | Exposed information                                  |
| ------------ | ---------------------------------------------------- |
| `rocketmq_broker_tps` | total put message numbers per second for this broker |
| `rocketmq_broker_qps` | total get message numbers per second for this broker |

**Metrics output example**

```txt
# HELP rocketmq_broker_tps BrokerPutNums
# TYPE rocketmq_broker_tps gauge
rocketmq_broker_tps{cluster="MQCluster",broker="broker-a",} 7.933333333333334
rocketmq_broker_tps{cluster="MQCluster",broker="broker-b",} 7.916666666666667
# HELP rocketmq_broker_qps BrokerGetNums
# TYPE rocketmq_broker_qps gauge
rocketmq_broker_qps{cluster="MQCluster",broker="broker-a",} 8.2
rocketmq_broker_qps{cluster="MQCluster",broker="broker-b",} 8.15
```

### Topics

**Metrics details**

| Name                | Exposed information                                |
| ------------------- | -------------------------------------------------- |
| `rocketmq_producer_tps`      | sending messages number per second  for this topic |
| `rocketmq_producer_put_size` | sending messages size per second  for this topic   |
| `rocketmq_producer_offset`   | Current Offset of a Broker for this topic          |

**Metrics output example**

```txt
# HELP rocketmq_producer_tps TopicPutNums
# TYPE rocketmq_producer_tps gauge
rocketmq_producer_tps{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 7.933333333333334
rocketmq_producer_tps{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 7.916666666666667
# HELP rocketmq_producer_put_size TopicPutSize
# TYPE rocketmq_producer_put_size gauge
rocketmq_producer_put_size{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 1642.2
rocketmq_producer_put_size{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 1638.75
# HELP rocketmq_producer_offset TopicOffset
# TYPE rocketmq_producer_offset counter
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="TBW102",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",} 1878633.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",} 3843787.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_20190304",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="BenchmarkTest",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_20190305",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="MQCluster",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 2798195.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="BenchmarkTest",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 1459666.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="MQCluster",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="SELF_TEST_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="OFFSET_MOVED_EVENT",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="broker-b",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="broker-a",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="SELF_TEST_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="RMQ_SYS_TRANS_HALF_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_20190305",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="OFFSET_MOVED_EVENT",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="RMQ_SYS_TRANS_HALF_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="TBW102",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_20190304",} 0.0

```

### Consumer Groups

**Metrics details**

| Name                              | Exposed information                                          |
| --------------------------------- | ------------------------------------------------------------ |
| `rocketmq_consumer_tps`                    | consumer message numbers per second for this Topic           |
| `rocketmq_consumer_get_size`               | consumer message size per second for this Topic              |
| `rocketmq_consumer_offset`                 | consumer offset for this topic                               |
| `rocketmq_group_get_latency`               | consumer latency on some topic for one queue                 |
| `rocketmq_group_get_latency_by_storetime ` | consumer latency between message consume time and message store time on some topic |

**Metrics output example**

```txt
# HELP rocketmq_consumer_tps GroupGetNums
# TYPE rocketmq_consumer_tps gauge
rocketmq_consumer_tps{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 7.916666666666667
rocketmq_consumer_tps{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 7.933333333333334
# HELP rocketmq_consumer_get_size GroupGetSize
# TYPE rocketmq_consumer_get_size gauge
rocketmq_consumer_get_size{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1638.75
rocketmq_consumer_get_size{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1642.2
# HELP rocketmq_consumer_offset GroupOffset
# TYPE rocketmq_consumer_offset counter
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1462030.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 3843787.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 2800569.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 1878633.0
# HELP rocketmq_group_get_latency GroupGetLatency
# TYPE rocketmq_group_get_latency gauge
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="0",} 0.05
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="1",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="7",} 0.05
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="6",} 0.016666666666666666
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="3",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="7",} 0.03333333333333333
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="4",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="5",} 0.03333333333333333
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="6",} 0.016666666666666666
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="2",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="3",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="0",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="4",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="1",} 0.03333333333333333
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="5",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="2",} 0.0
# HELP rocketmq_group_get_latency_by_storetime GroupGetLatencyByStoreTime
# TYPE rocketmq_group_get_latency_by_storetime gauge
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 3215.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 0.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 3232.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 0.0
```
