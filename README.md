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

