# rocketmq-replicator

## rocketmq-replicator打包
````
mvn clean install -Prelease-all -DskipTest -U 
打包成功后将rocketmq-replicator-0.1.0-SNAPSHOT-jar-with-dependencies.jar（fatjar）放到runtime配置的pluginPaths目录下
````
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
