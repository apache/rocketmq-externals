Table of Contents  
=================

   * [1. Rocketmq-redis-replicator](#1-rocketmq-redis-replicator)
      * [1.1. Brief introduction](#11-brief-introduction)
      * [1.2. Architecture](#12-architecture)
   * [2. Install](#2-install)
      * [2.1. Requirements](#21-requirements)
      * [2.2. Install from source code](#22-install-from-source-code)
   * [3. Simple usage](#3-simple-usage)
      * [3.1. Downstream via socket](#31-downstream-via-socket)
      * [3.2. Deploy as an independent service](#32-deploy-as-an-independent-service)
      * [3.3. Consume redis event](#33-consume-redis-event)
   * [4. Configuration](#4-configuration)
      * [4.1. Rocketmq configuration](#41-rocketmq-configuration)
      * [4.2. Specify your own configuration](#42-specify-your-own-configuration)
   * [5. Other topics](#5-other-topics)
      * [5.1. Built-in command parser](#51-built-in-command-parser)
      * [5.2. EOFException](#52-eofexception)
      * [5.3. Trace event log](#53-trace-event-log)
      * [5.4. Auth](#54-auth)
      * [5.5. Avoid full sync](#55-avoid-full-sync)  
      
# 1. Rocketmq-redis-replicator  

## 1.1. Brief introduction

Rocketmq redis replicator implement Redis Replication protocol written in java. It can parse, filter, broadcast the RDB and AOF events in a real time manner and downstream these event to RocketMQ.  

## 1.2. Architecture

```java  



+-------+     PSNC      +--------------+
|       |<--------------|              |   event      +--------------+
| Redis |               |              |------------->|              |
|       |-------------->|Rocketmq-redis|   event      |              |
+-------+     data      | (parse data) |------------->|   Rocketmq   |
                        |              |   event      |              |
                        |              |------------->|              |
                        +--------------+              +--------------+

```

# 2. Install  
## 2.1. Requirements  
jdk 1.8+  
maven-3.3.1+  
redis 2.6 - 5.0.x  
rocketmq 4.2.0 or higher  

## 2.2. Install from source code  
  
```
    $mvn clean install package -Dmaven.test.skip=true
```  

# 3. Simple usage  
  
## 3.1. Downstream via socket  
  
```java  
        Configure configure = new Configure();
        Replicator replicator = new RocketMQRedisReplicator(configure);
        final RocketMQRedisProducer producer = new RocketMQRedisProducer(configure);
        producer.open();
        replicator.addEventListener(new EventListener() {
            @Override public void onEvent(Replicator replicator, Event event) {
                try {
                    if (!producer.send(event)) {
                        LOGGER.error("Failed to send Event");
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to send Event", e);
                }
            }
        });
        
        replicator.addCloseListener(new CloseListener() {
            @Override public void handle(Replicator replicator) {
                producer.close();
            }
        });

        replicator.open();
```
## 3.2. Deploy as an independent service
1. `mvn clean package -Dmaven.test.skip`
2. `sh target/rocketmq-redis-pack/bin/start.sh`

## 3.3. Consume redis event

```java  

        Configure configure = new Configure();
        RocketMQRedisConsumer consumer = new RocketMQRedisConsumer(configure);
        consumer.addEventListener(new EventListener() {
            @Override public void onEvent(Event event) {
                if (event instanceof PreRdbSyncEvent) {
                    // pre rdb sync
                    // your code goes here
                } else if (event instanceof AuxField) {
                    // rdb aux field event
                    // your code goes here
                } else if (event instanceof KeyValuePair) {
                    // rdb event
                    // your code goes here
                } else if (event instanceof PostRdbSyncEvent) {
                    // post full sync
                    // your code goes here
                } else if (event instanceof Command) {
                    // aof command event
                    // your code goes here
                } else if (event instanceof PreCommandSyncEvent) {
                    // pre command sync
                    // your code goes here
                } else if (event instanceof PostCommandSyncEvent) {
                    // post command sync
                    // your code goes here
                }
            }
        });
        consumer.open();

```  

# 4. Configuration

The config file located at target/rocketmq-redis-pack/conf/replicator.conf

## 4.1. Rocketmq configuration  

| parameter | default value| detail |
|-----------|--------------|--------|
| rocketmq.nameserver.address | 127.0.0.1:9876 | rocketmq server address|  
| rocketmq.producer.groupname | REDIS_REPLICATOR_PRODUCER_GROUP | rocketmq producer group name |  
| rocketmq.consumer.groupname | REDIS_REPLICATOR_CONSUMER_GROUP | rocketmq consumer group name |  
| rocketmq.data.topic | redisdata | rocketmq topic name |  
| deploy.model | single | single or cluster |  
| zookeeper.address | 127.0.0.1:2181 | run on cluster model |  
| redis.uri | redis://127.0.0.1:6379 | the uri of redis master which replicate from |  

## 4.2. Specify your own configuration

By default the configuration file `replicator.conf` loaded from your classpath.  
But you can specify your own configuration using `Configure` like following:  

```java  

        Properties properties = new Properties()
        properties.setProperty("zookeeper.address", "127.0.0.1:2181");
        properties.setProperty("redis.uri", "redis://127.0.0.1:6379");
        properties.setProperty("rocketmq.nameserver.address", "localhost:9876");
        properties.setProperty("rocketmq.producer.groupname", "REDIS_REPLICATOR_PRODUCER_GROUP");
        properties.setProperty("rocketmq.consumer.groupname", "REDIS_REPLICATOR_CONSUMER_GROUP");
        properties.setProperty("rocketmq.data.topic", "redisdata");
        properties.setProperty("deploy.model", "single");
        Configure configure = new Configure(properties);
        
```

# 5. Other topics  
  
## 5.1. Built-in command parser  


|**commands**|**commands**  |  **commands**  |**commands**|**commands**  | **commands**       |
| ---------- | ------------ | ---------------| ---------- | ------------ | ------------------ |    
|  **PING**  |  **APPEND**  |  **SET**       |  **SETEX** |  **MSET**    |  **DEL**           |  
|  **SADD**  |  **HMSET**   |  **HSET**      |  **LSET**  |  **EXPIRE**  |  **EXPIREAT**      |  
| **GETSET** | **HSETNX**   |  **MSETNX**    | **PSETEX** | **SETNX**    |  **SETRANGE**      |  
| **HDEL**   | **UNLINK**   |  **SREM**      | **LPOP**   |  **LPUSH**   | **LPUSHX**         |  
| **LRem**   | **RPOP**     |  **RPUSH**     | **RPUSHX** |  **ZREM**    |  **ZINTERSTORE**   |  
| **INCR**   |  **DECR**    |  **INCRBY**    |**PERSIST** |  **SELECT**  | **FLUSHALL**       |  
|**FLUSHDB** |  **HINCRBY** | **ZINCRBY**    | **MOVE**   |  **SMOVE**   |**BRPOPLPUSH**      |  
|**PFCOUNT** |  **PFMERGE** | **SDIFFSTORE** |**RENAMENX**| **PEXPIREAT**|**SINTERSTORE**     |  
|**ZADD**    | **BITFIELD** |**SUNIONSTORE** |**RESTORE** | **LINSERT**  |**ZREMRANGEBYLEX**  |  
|**GEOADD**  | **PEXPIRE**  |**ZUNIONSTORE** |**EVAL**    |  **SCRIPT**  |**ZREMRANGEBYRANK** |  
|**PUBLISH** |  **BITOP**   |**SETBIT**      | **SWAPDB** | **PFADD**    |**ZREMRANGEBYSCORE**|  
|**RENAME**  |  **MULTI**   |  **EXEC**      | **LTRIM**  |**RPOPLPUSH** |     **SORT**       |  
|**EVALSHA** | **ZPOPMAX**  | **ZPOPMIN**    | **XACK**   | **XADD**     |  **XCLAIM**        |  
|**XDEL**    | **XGROUP**   | **XTRIM**      |**XSETID**  |              |                    |  
  
## 5.2. EOFException
  
* Adjust redis server setting like the following. more details please refer to [redis.conf](https://raw.githubusercontent.com/antirez/redis/3.0/redis.conf)  
  
```java  
    client-output-buffer-limit slave 0 0 0
```  
**WARNNING: this setting may run out of memory of redis server in some cases.**  
  
## 5.3. Trace event log  
  
* If you are using log4j2, add logger like the following:

```xml  
    <Logger name="com.moilioncircle" level="info">
        <AppenderRef ref="YourAppender"/>
    </Logger>
```
  
```java  
    // redis uri
    "redis://127.0.0.1:6379?verbose=yes"
```

## 5.4. Auth  
  
```java  
    // redis uri
    "redis://127.0.0.1:6379?authPassword=foobared"
```  

## 5.5. Avoid full sync  
  
* Adjust redis server setting like the following  
  
```java  
    repl-backlog-size
    repl-backlog-ttl
    repl-ping-slave-periods
```
`repl-ping-slave-period` **MUST** less than `readTimeout`, default `readTimeout` is 30 seconds
