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

   



- b、日志相关配置在logback.xml中修改

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

如果看到以下日志说明runttiime启动成功了

2019-07-16 10:56:24 INFO RebalanceService - RebalanceService service started
2019-07-16 10:56:24 INFO main - The worker [DEFAULT_WORKER_1] boot success.

2、启动sourceConnector

​	正在做测试（To be continued）已实现Bulk Mode

cd target/distribution/

java -cp .;./conf/;./lib/* org.apache.rocketmq.connect.runtime.ConnectStartup -c conf/connect.conf



在http中输入Get 请求



示例

[http://127.0.0.1:8085/connectors/testSourceConnector1?config={"connector-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","jdbcUrl":"127.0.0.1:3306","jdbcUsername":"root","jdbcPassword":"123456","task-class":"org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector","rocketmqTopic":"jdbcTopic","mode":"bulk","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}](http://127.0.0.1:8085/connectors/testSourceConnector1?config={%22connector-class%22:%22org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector%22,%22jdbcUrl%22:%22127.0.0.1:3306%22,%22jdbcUsername%22:%22root%22,%22jdbcPassword%22:%22199812160%22,%22task-class%22:%22org.apache.rocketmq.connect.jdbc.connector.JdbcSourceConnector%22,%22rocketmqTopic%22:%22jdbcTopic%22,%22mode%22:%22bulk%22,%22source-record-converter%22:%22org.apache.rocketmq.connect.runtime.converter.JsonConverter%22})









