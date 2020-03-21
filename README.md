# rocketmq-replicator

## rocketmq-replicator简介

![image](https://blobscdn.gitbook.com/v0/b/gitbook-28427.appspot.com/o/assets%2F-Lm4-doAUYYZgDcb_Jnz%2F-LoOhyGfSf-N6oHVgJhr%2F-LoOi0ADfZ4q-qPo_uEB%2Frocketmq%20connector.png?alt=media&token=0bbbfa54-240a-489e-8dfb-1996d0800dfc)

Replicator用于RocketMQ集群之间的信息同步，作为一个connector运行在RocketMQ Runtime上之上, 能够同步两个独立的RocketMQ集群之间的消息。

## 中文文档

[Replicator文档](https://rocketmq-1.gitbook.io/rocketmq-connector/rocketmq-connector/replicator/replicator-jian-jie)

[快速开始](https://rocketmq-1.gitbook.io/rocketmq-connector/rocketmq-connector/replicator/rocketmq-replicator)

---

# replicator使用说明

## rocketmq-replicator打包
````
mvn clean install -Prelease-all -DskipTest -U 
````

打包成功后将`rocketmq-replicator-0.1.0-SNAPSHOT-jar-with-dependencies.jar`（fatjar）放到runtime配置的pluginPaths目录下

## rocketmq-replicator启动

同步topic和消息
````
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-replicator-name}
?config={"connector-class":"org.apache.rocketmq.replicator.RmqSourceReplicator","source-rocketmq":"xxxx:9876","target-rocketmq":"xxxxxxx:9876","replicator-store-topic":"replicatorTopic","taskDivideStrategy":"0","white-list":"TopicTest,TopicTest2","task-parallelism":"2","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
````


## rocketmq-replicator停止
````
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-replicator-name}/stop
````

## rocketmq-meta-connector启动

同步消费消费进度和ConsumerGroup

注：此功能尚不成熟还需要后续版本优化
````
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-replicator-name}
?config={"connector-class":"org.apache.rocketmq.replicator.RmqMetaReplicator","source-rocketmq":"xxxx:9876","target-rocketmq":"xxxxxxx:9876","replicator-store-topic":"replicatorTopic","offset.sync.topic":"syncTopic","taskDivideStrategy":"0","white-list":"TopicTest,TopicTest2","task-parallelism":"2","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
````


## rocketmq-rocketmq-connector停止
````
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-replicator-name}/stop
````

## rocketmq-replicator参数说明

parameter | type | must | description | sample value
---|---|---|---|---|
source-rocketmq | String | Yes | namesrv address of source rocketmq cluster | 192.168.1.2:9876 |
target-rocketmq | String | Yes | namesrv address of target rocketmq cluster | 192.168.1.2:9876 |
target-cluster | String | Yes | target rocketmq cluster name | DefaultCluster |
source-cluster | String | Yes | source rocketmq cluster name | DefaultCluster |
replicator-store-topic | String | Yes | topic name to store all source messages | replicator-store-topic |
task-divide-strategy | Integer | No | task dividing strategy, default value is 0 for dividing by topic | 0 |
white-list | String | Yes | topic white list and multiple fields are separated by commas | topic-1,topic-2 |
task-parallelism | String | No | task parallelism，default value is 1，one task will be responsible for multiple topics for the value greater than 1 | 2 |
source-record-converter | String | Yes | source data parser | io.openmessaging.connect.runtime.converter.JsonConverter |
topic.rename.format | String | Yes | rename topic name rules | rename-${topic} (${topic} represents the source topic name) |
