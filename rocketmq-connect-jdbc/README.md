# RocketMQ-connect-jdbc

### Directory Structure Description

```web-idl
│  pom.xml
│  README.md
└─src
    ├─main
    │  └─java
    │      └─org
    │          └─apache
    │              └─rocketmq
    │                  └─connect
    │                      └─jdbc
    │                          │  Config.java
    │                          ├─connector
    │                          │      JdbcSourceConnector.java
    │                          │      JdbcSourceTask.java
    │                          ├─dialect
    │                          ├─schema
    │                          │  │  Database.java
    │                          │  │  Schema.java
    │                          │  │  Table.java
    │                          │  │
    │                          │  └─column
    │                          │          BigIntColumnParser.java
    │                          │          ColumnParser.java
    │                          │          DateTimeColumnParser.java
    │                          │          DefaultColumnParser.java
    │                          │          EnumColumnParser.java
    │                          │          IntColumnParser.java
    │                          │          SetColumnParser.java
    │                          │          StringColumnParser.java
    │                          │          TimeColumnParser.java
    │                          │          YearColumnParser.java
    │                          ├─sink
    │                          └─source
    │                                  Querier.java
    └─test
        └─java
            └─org
                └─apache
                    └─rocketmq
                        └─connect
                            └─jdbc
                                └─connector
                                        JdbcSourceConnectorTest.java
                                        JdbcSourceTaskTest.java
```

### Some Result of Testing JdbcSourceTask



#### Data Type：SourceDataEntry

{sourcePartition,sourcePosition,DataEntry{timestamp,entryType=CREATE,queueName,shardingKey,schema.schema=Schema{dataSource=DATABASE_NAME,name=TABLE_NAME,fields=[Field{index,name,type}]},payloading}}

- For example

```javascript
    SourceDataEntry{sourcePartition=java.nio.HeapByteBuffer[pos=0 lim=14 cap=14], sourcePosition=java.nio.HeapByteBuffer[pos=0 lim=44 cap=44]} DataEntry{timestamp=1564397062419, entryType=CREATE, queueName='student', shardingKey='null', 
    schema=Schema{dataSource='jdbc_db', name='student', fields=[Field{index=0, name='id', type=INT32}, Field{index=1, name='first', type=STRING}, 
    Field{index=2, name='last', type=STRING}, Field{index=3, name='age', type=INT32}]}, payload=[102121, "Python", "Py", 25]}
```

#### Mentioned DataBase Information and all SourceDataEntry

- For example

![database.png](https://github.com/yuchenlichuck/picture/blob/master/database.png?raw=true)

![sourcedataentry.png](https://github.com/yuchenlichuck/picture/blob/master/sourcedataentry.png?raw=true)

**启动Connector**

[http://127.0.0.1:8081/connectors/connector-name?config={"connector-class":"org.apache.rocketmq.connect.kafka.connector.KafkaSourceConnector","oms-driver-url":"oms](http://127.0.0.1:8081/connectors/connector-name?config=%7B%22connector-class%22:%22org.apache.rocketmq.connect.kafka.connector.KafkaSourceConnector%22,%22oms-driver-url%22:%22oms): rocketmq://127.0.0.1:9876/default:default","tasks.num":"1","kafka.topics":"test1,test2","kafka.group.id":"group0","kafka.bootstrap.server":"127.0.0.1:9092","source-record-converter":"io.openmessaging.connect.runtime.converter.JsonConverter"}

**查看Connector运行状态**

<http://127.0.0.1:8081/connectors/connector-name/status>

**查看Connector配置**

<http://127.0.0.1:8081/connectors/connector-name/config>

**关闭Connector**

<http://127.0.0.1:8081/connectors/connector-name/stop>







# JDBC Connector 构建

![dataflow](https://github.com/openmessaging/openmessaging-connect/raw/master/flow.png)

#### 一、下载rocketmq-connect-runtime

```
1、git clone https://github.com/apache/rocketmq-externals.git

2、cd rocketmq-externals/rocketmq-connect-runtime

3、mvn -Dmaven.test.skip=true package

4、cd target/distribution/conf
```

- a、修改connect.conf配置文件

```
#1、rocketmq 配置
namesrvAddr=127.0.0.1:9876
   
#2、file-connect jar包路径
pluginPaths=/home/connect/file-connect/target
   
#3、runtime持久化文件目录
storePathRootDir=/home/connect/storeRoot
   
#4、http服务端口
httpPort=8081
```

b、日志相关配置在logback.xml中修改

```
注：rocketmq需要先创建cluster-topic，config-topic，offset-topic，position-topic
4个topic，并且为了保证消息有序，每个topic可以只一个queue
```

### 二、启动Connector

1、启动runtime
回到rocketmq-externals/rocketmq-connect-runtime目录

```
./run_worker.sh
```

看到日志目录查看connect_runtime.log

windows用户可以用CMD到程序根目录下再输入：

```
cd target/distribution/

java -cp .;./conf/;./lib/* org.apache.rocketmq.connect.runtime.ConnectStartup -c conf/connect.conf
```

如果看到以下日志说明runttiime启动成功了

2019-07-16 10:56:24 INFO RebalanceService - RebalanceService service started
2019-07-16 10:56:24 INFO main - The worker [DEFAULT_WORKER_1] boot success.

2、启动sourceConnector

```
1、git clone https://github.com/apache/rocketmq-externals.git

2、cd rocketmq-externals/rocketmq-connect-jdbc

3、mvn -Dmaven.test.skip=true package

```

- 复制第三方jar至target

```
mvn dependency:copy-dependencies
```

#### Bulk查询方法，在http中输入Get 请求（目前仅适配过MYSQL）

请将jdbcUrl, jdbcUsername, jdbcPassword，改为所连接数据库的配置

```http
http://127.0.0.1:8081/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","jdbcUrl":"127.0.0.1:3306","jdbcUsername":"root","jdbcPassword":"123456","task-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","rocketmqTopic":"jdbcTopic","mode":"bulk","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

看到一下日志说明Jdbc source connector启动成功了

2019-08-09 11:33:22 INFO RebalanceService - JdbcSourceConnector verifyAndSetConfig enter
2019-08-09 11:33:23 INFO pool-9-thread-1 - Config.load.start
2019-08-09 11:33:23 INFO pool-9-thread-1 - querier.start
2019-08-09 11:33:23 INFO pool-9-thread-1 - {password=199812160, validationQuery=SELECT 1 FROM DUAL, testWhileIdle=true, timeBetweenEvictionRunsMillis=60000, minEvictableIdleTimeMillis=300000, initialSize=2, driverClassName=com.mysql.cj.jdbc.Driver, maxWait=60000, url=jdbc:mysql://localhost:3306?useSSL=true&verifyServerCertificate=false&serverTimezone=GMT%2B8, username=root, maxActive=2},config read successful
2019-08-09 11:33:24 INFO RebalanceService - JdbcSourceConnector verifyAndSetConfig enter
2019-08-09 11:33:25 INFO pool-9-thread-1 - {dataSource-1} inited
2019-08-09 11:33:27 INFO pool-9-thread-1 - schema load successful
2019-08-09 11:33:27 INFO pool-9-thread-1 - querier.poll

#### Incrementing and / or Timestamp Querier

- 要使用的时间戳和/或自增列必须在连接器处理的所有表上。如果不同的表具有不同名称的时间戳/自增列，则需要创建单独的连接器配置；
- 可以结合使用这些方法中的（时间戳/自增）或两者（时间戳+自增）；

##### Incrementing Querier （自增列查询）

注：表中需要含递增的一列，如果只使用自增列，则不会捕获对数据的更新，除非每次更新时自增列也会增加。

```http
http://127.0.0.1:8081/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","jdbcUrl":"127.0.0.1:3306","jdbcUsername":"root","jdbcPassword":"123456","task-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","rocketmqTopic":"jdbcTopic","mode":"incrementing","incrementingColumnName":"id,"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

##### Timestamp Querier （时间戳查询）


```http
http://127.0.0.1:8081/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","jdbcUrl":"127.0.0.1:3306","jdbcUsername":"root","jdbcPassword":"123456","task-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","rocketmqTopic":"jdbcTopic","mode":"timestamp":"timestampColumnName","id","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

##### Incrementing + Timestamp Querier (自增列+时间戳查询) 


```http
http://127.0.0.1:8081/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","jdbcUrl":"127.0.0.1:3306","jdbcUsername":"root","jdbcPassword":"123456","task-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","rocketmqTopic":"jdbcTopic","mode":"incrementing+timestamp","timestampColumnName":"timestamp","incrementingColumnName":"id","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}
```

日志如果显示为如下，则connect成功。

2019-08-15 14:09:43 INFO RebalanceService - JdbcSourceConnector verifyAndSetConfig enter
2019-08-15 14:09:44 INFO pool-14-thread-1 - Config.load.start
2019-08-15 14:09:44 INFO pool-14-thread-1 - config load successfully
2019-08-15 14:09:44 INFO pool-14-thread-1 - map start,199812160
2019-08-15 14:09:44 INFO pool-14-thread-1 - {password=199812160, validationQuery=SELECT 1 FROM DUAL, testWhileIdle=true, timeBetweenEvictionRunsMillis=60000, minEvictableIdleTimeMillis=300000, initialSize=2, driverClassName=com.mysql.cj.jdbc.Driver, maxWait=60000, url=jdbc:mysql://127.0.0.1:3306?useSSL=true&verifyServerCertificate=false&serverTimezone=GMT%2B8, username=root, maxActive=2}config read successfully

3、启动sinkConnector

To Be Continued.

