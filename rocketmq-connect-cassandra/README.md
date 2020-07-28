# rocketmq-connect-cassandra

## rocketmq-connect-cassandra 打包
```
mvn clean install -DskipTest -U 
```

## 目前安装会遇到的问题

目前的rocketmq-connect-cassandra 使用的是datastax-java-driver:4.5.0版本的cassandra-driver，由于在打包过程中还有没有解决的问题，该cassandra driver无法读取
位于driver包中的默认配置文件，因此我们需要手动下载cassandra driver的配置文件[reference.conf](https://github.com/datastax/java-driver/blob/4.5.0/core/src/main/resources/reference.conf) 并将其放置于classpath中。

该问题还仍然在解决的过程中。

## rocketmq-connect-cassandra 启动

* **cassandra-source-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-cassandra-source-connector-name}
?config={"connector-class":"org.apache.rocketmq.connect.cassandra.connector.JdbcSourceConnector",“dbUrl”:"${source-db-ip}",dbPort”:"${source-db-port}",dbUsername”:"${source-db-username}",dbPassword”:"${source-db-password}","rocketmqTopic":"cassandraTopic","mode":"bulk","whiteDataBase":{"${source-db-name}":{"${source-table-name}":{"${source-column-name}":"${source-column-value}"}}},"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

* **cassandra-sink-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-cassandra-sink-connector-name}
?config={"connector-class":"org.apache.rocketmq.connect.cassandra.connector.JdbcSinkConnector",“dbUrl”:"${sink-db-ip}",dbPort”:"${sink-db-port}",dbUsername”:"${sink-db-username}",dbPassword”:"${sink-db-password}","rocketmqTopic":"cassandraTopic","mode":"bulk","topicNames":"${sink-topic-name}","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```
>**注：** `rocketmq-cassandra-connect` 的启动依赖于`rocketmq-connect-runtime`项目的启动，需将打好的`jar`包放置到`runtime`项目中`pluginPaths`配置的路径后再执行上面的启动请求,该值配置在`runtime`项目下的`connect.conf`文件中

## rocketmq-connect-cassandra 停止

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-cassandra-connector-name}/stop
```

## rocketmq-connect-cassandra 参数说明
* **cassandra-source-connector 参数说明**

参数 | 类型 | 是否必须 | 描述 | 样例
|---|---|---|---|---|
|dbUrl | String | 是 | source端 DB ip | 192.168.1.2|
|dbPort | String | 是 | source端 DB port | 3306 |
|dbUsername | String | 是 | source端 DB 用户名 | root |
|dbPassword | String | 是 | source端 DB 密码 | 123456 |
|rocketmqTopic | String | 是 | 待废弃的参数，需和topicNames相同 | jdbc_cassandra |
|topicNames | String | 是 | rocketmq默认每一个数据源中的表对应一个名字，该名称需和数据库表名称相同 | jdbc_cassandra |
|whiteDataBase | String | 是 | source端同步数据白名单，嵌套配置，为{DB名：{表名：{字段名：字段值}}}，若无指定字段数据同步，字段名可设为NO-FILTER，值为任意 | {"DATABASE_TEST":{"TEST_DATA":{"name":"test"}}} |
|mode | String | 是 | source-connector 模式，目前仅支持bulk | bulk |
|localDataCenter | String | 是 | 待废弃 | cassandra 集群的datacenter名称，为必填项 |
|task-divide-strategy | Integer | 否 | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism | Integer | 否 | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-cluster | String | 是 | sink 端获取路由信息连接到的RocketMQ nameserver 地址 | 172.17.0.1:10911 |
|source-rocketmq | String | 是 | sink 端获取路由信息连接到的RocketMQ broker cluster 地址 | 127.0.0.1:9876 |
|source-record-converter | String | 是 | source data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |


示例配置如下
```js
{
    "connector-class":"org.apache.rocketmq.connect.cassandra.connector.CassandraSourceConnector",
    "rocketmqTopic":"jdbc_cassandra",
    "topicNames": "jdbc_cassandra",
    "dbUrl":"127.0.0.1",
    "dbPort":"9042",
    "dbUsername":"cassandra",
    "dbPassword":"cassandra",
    "localDataCenter":"datacenter1",
    "whiteDataBase": {
        "jdbc":{
           "jdbc_cassandra": {"NO-FILTER": "10"}
         }
      },
    "mode": "bulk",
    "task-parallelism": 1,
    "source-cluster": "172.17.0.1:10911",
    "source-rocketmq": "127.0.0.1:9876",
    "source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
 }
```
* **cassandra-sink-connector 参数说明**

参数 | 类型 | 是否必须 | 描述 | 样例
|---|---|---|---|---|
|dbUrl | String | 是 | sink端 DB ip | 192.168.1.2|
|dbPort | String | 是 | sink端 DB port | 3306 |
|dbUsername | String | 是 | sink端 DB 用户名 | root |
|dbPassword | String | 是 | sink端 DB 密码 | 123456 |
|topicNames | String | 是 | sink端同步数据的topic名字 | topic-1,topic-2 |
|mode | String | 是 | source-connector 模式，目前仅支持bulk | bulk |
|~~rocketmqTopic~~ | String | 是 | 待废弃 | cassandraTopic |
|task-divide-strategy | Integer | 否 | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism | Integer | 否 | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-rocketmq | String | 是 | sink 端获取路由信息连接到的RocketMQ nameserver 地址 | 172.17.0.1:10911 |
|source-cluster | String | 是 | sink 端获取路由信息连接到的RocketMQ broker cluster 地址 | 127.0.0.1:9876 |
|source-record-converter | String | 是 | source data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |
