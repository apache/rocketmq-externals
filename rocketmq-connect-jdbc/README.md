# rocketmq-connect-jdbc

## rocketmq-connect-jdbc 打包
```
mvn clean install -Dmaven.test.skip=true
```

## rocketmq-connect-jdbc 启动

* **jdbc-source-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-source-connector-name}
?config={"source-rocketmq":"${runtime-ip}:${runtime-port}","source-cluster":"${broker-cluster}","connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector",“dbUrl”:"${source-db-ip}",dbPort”:"${source-db-port}",dbUsername”:"${source-db-username}",dbPassword”:"${source-db-password}","rocketmqTopic":"${source-table-name}","mode":"bulk","whiteDataBase":{"${source-db-name}":{"${source-table-name}":{"${source-column-name}":"${source-column-value}"}}},"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

例子

```
http://localhost:8081/connectors/jdbcConnectorSource?config={"source-rocketmq":"localhost:9876","source-cluster":"DefaultCluster",
"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","dbUrl":"192.168.1.3","dbPort":"3306","dbUsername":"root","dbPassword":"mysqldb123456",
"rocketmqTopic":"test_table","mode":"bulk","whiteDataBase":{"test_database":{"test_table":{"test_column":"8"}}},
"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

* **jdbc-sink-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-sink-connector-name}
?config={"source-rocketmq":"${runtime-ip}:${runtime-port}","source-cluster":"${broker-cluster}","connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSinkConnector",“dbUrl”:"${sink-db-ip}",dbPort”:"${sink-db-port}",dbUsername”:"${sink-db-username}",dbPassword”:"${sink-db-password}","mode":"bulk","topicNames":"${source-table-name}","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

例子 
```
http://localhost:8081/connectors/jdbcConnectorSink?config={"source-rocketmq":"localhost:9876","source-cluster":"DefaultCluster",
"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSinkConnector","dbUrl":"192.168.1.2","dbPort":"3306","dbUsername":"root",
"dbPassword":"mysqldb123456","topicNames":"test_table","mode":"bulk","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

>**注：** `rocketmq-jdbc-connect` 的启动依赖于`rocketmq-connect-runtime`项目的启动，需将打好的所有`jar`包放置到`runtime`项目中`pluginPaths`配置的路径后再执行上面的启动请求,该值配置在`runtime`项目下的`connect.conf`文件中

## rocketmq-connect-jdbc 停止

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-jdbc-connector-name}/stop
```

## rocketmq-connect-jdbc 参数说明
* **jdbc-source-connector 参数说明**

|         KEY            |  TYPE   | Must be filled | Description| Example
|------------------------|---------|----------------|------------|---|
|dbUrl                   | String  | YES            | source端 DB ip | 192.168.1.3|
|dbPort                  | String  | YES            | source端 DB port | 3306 |
|dbUsername              | String  | YES            | source端 DB 用户名 | root |
|dbPassword              | String  | YES            | source端 DB 密码 | 123456 |
|whiteDataBase           | String  | YES            | source端同步数据白名单，嵌套配置，为{DB名：{表名：{字段名：字段值}}}，若无指定字段数据同步，字段名可设为NO-FILTER，值为任意 | {"DATABASE_TEST":{"TEST_DATA":{"name":"test"}}} |
|mode                    | String  | YES            | source-connector 模式，目前仅支持bulk | bulk |
|rocketmqTopic           | String  | NO             | source端同步数据的topic名字，必须和要同步的数据库表名一样 | TEST_DATA |
|task-divide-strategy    | Integer | NO             | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism        | Integer | NO             | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-rocketmq         | String  | YES            | source 端获取路由信息连接到的RocketMQ nameserver 地址 | 192.168.1.3:9876 |
|source-cluster          | String  | YES            | source 端获取路由信息连接到的RocketMQ broker cluster | DefaultCluster |
|source-record-converter | String  | YES            | source data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |

```  
注：1. source/sink配置文件说明是以rocketmq-connect-jdcb为demo，不同source/sink connector配置有差异，请以具体sourc/sink connector为准
    2. rocketmqTopic 在jdbc-source-connector中没有被用到，暂时保留的原因是为了配置显示一致性
```  
* **jdbc-sink-connector 参数说明**

|         KEY            |  TYPE   | Must be filled | Description| Example
|------------------------|---------|----------------|------------|---|
|dbUrl                   | String  | YES            | sink端 DB ip | 192.168.1.2|
|dbPort                  | String  | YES            | sink端 DB port | 3306 |
|dbUsername              | String  | YES            | sink端 DB 用户名 | root |
|dbPassword              | String  | YES            | sink端 DB 密码 | 123456 |
|mode                    | String  | YES            | source-connector 模式，目前仅支持bulk | bulk |
|topicNames              | String  | YES            | sink端同步数据的topic名字，必须和要同步的数据库表名一样 | TEST_DATA |
|task-divide-strategy    | Integer | NO             | sink端 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism        | Integer | NO             | sink端 parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-rocketmq         | String  | YES            | sink端 端获取路由信息连接到的RocketMQ nameserver 地址 | 192.168.1.3:9876 |
|source-cluster          | String  | YES            | sink端 端获取路由信息连接到的RocketMQ broker cluster | DefaultCluster |
|source-record-converter | String  | YES            | sink端 data 解析 | org.apache.rocketmq.connect.runtime.converter.JsonConverter |

