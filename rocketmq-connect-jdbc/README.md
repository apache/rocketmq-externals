# rocketmq-connect-jdbc

## rocketmq-connect-jdbc 打包
```
mvn clean install -DskipTest -U 
```

## rocketmq-connect-jdbc 启动

* **jdbc-source-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-source-connector-name}
?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector",“dbUrl”:"${source-db-ip}",dbPort”:"${source-db-port}",dbUsername”:"${source-db-username}",dbPassword”:"${source-db-password}","rocketmqTopic":"jdbcTopic","mode":"bulk","whiteDataBase":{"${source-db-name}":{"${source-table-name}":{"${source-column-name}":"${source-column-value}"}}},"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

* **jdbc-sink-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-sink-connector-name}
?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSinkConnector",“dbUrl”:"${sink-db-ip}",dbPort”:"${sink-db-port}",dbUsername”:"${sink-db-username}",dbPassword”:"${sink-db-password}","rocketmqTopic":"jdbcTopic","mode":"bulk","topicNames":"${sink-topic-name}","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```
>**注：** `rocketmq-jdbc-connect` 的启动依赖于`rocketmq-connect-runtime`项目的启动，需将打好的`jar`包放置到`runtime`项目中`pluginPaths`配置的路径后再执行上面的启动请求,该值配置在`runtime`项目下的`connect.conf`文件中

## rocketmq-connect-jdbc 停止

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-connector-name}/stop
```

## rocketmq-connect-jdbc 参数说明
* **jdbc-source-connector 参数说明**

参数 | 类型 | 是否必须 | 描述 | 样例
|---|---|---|---|---|
|dbUrl | String | 是 | source端 DB ip | 192.168.1.2|
|dbPort | String | 是 | source端 DB port | 3306 |
|dbUsername | String | 是 | source端 DB 用户名 | root |
|dbPassword | String | 是 | source端 DB 密码 | 123456 |
|whiteDataBase | String | 是 | source端同步数据白名单，嵌套配置，为{DB名：{表名：{字段名：字段值}}}，若无指定字段数据同步，字段名可设为NO-FILTER，值为任意 | {"DATABASE_TEST":{"TEST_DATA":{"name":"test"}}} |
|mode | String | 是 | source-connector 模式，目前仅支持bulk | bulk |
|~~rocketmqTopic~~ | String | 是 | 待废弃 | jdbcTopic |
|task-divide-strategy | Integer | 否 | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism | Integer | 否 | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-record-converter | String | 是 | source data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |

* **jdbc-sink-connector 参数说明**

参数 | 类型 | 是否必须 | 描述 | 样例
|---|---|---|---|---|
|dbUrl | String | 是 | sink端 DB ip | 192.168.1.2|
|dbPort | String | 是 | sink端 DB port | 3306 |
|dbUsername | String | 是 | sink端 DB 用户名 | root |
|dbPassword | String | 是 | sink端 DB 密码 | 123456 |
|topicNames | String | 是 | sink端同步数据的topic名字 | topic-1,topic-2 |
|mode | String | 是 | source-connector 模式，目前仅支持bulk | bulk |
|~~rocketmqTopic~~ | String | 是 | 待废弃 | jdbcTopic |
|task-divide-strategy | Integer | 否 | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism | Integer | 否 | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-rocketmq | String | 是 | sink 端获取路由信息连接到的RocketMQ nameserver 地址 | TODO |
|source-rocketmq | String | 是 | sink 端获取路由信息连接到的RocketMQ broker cluster 地址 | TODO |
|source-record-converter | String | 是 | source data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |
