# RocketMQ-HBase Sink

## Overview

This project replicates HBase tables to RocketMQ topics.

## Pre-requisites
- HBase 1.2+
- JDK 1.8+
- RocketMQ 4.0.0+ 

## Requirements

- HBase cluster is configured with the setting `hbase.replication` to `true` in hbase-site.xml

## Properties
Have the below properties set in `hbase-site.xml` and add it to the HBase region server classpath.

|key               |nullable|default    |description|
|------------------|--------|-----------|-----------|
|rocketmq.namesrv.addr     |false   |           |RocketMQ name server address (e.g.,127.0.0.1:9876)|
|rocketmq.topic    |  false |  | RocketMQ topic to replicate HBase data to. | 
|rocketmq.hbase.tables  |false   |           | List of HBase tables, separated by comma, to replicate to RocketMQ (e.g., table1,table2,table3)|


## Deployment
1. Add rocketmq-hbase-X.Y-SNAPSHOT.jar and hbase-site.xml with the required properties to all the HBase region servers classpath and restart them.

2. At HBase shell, run the following commands.

```bash
hbase> create 'test', {NAME => 'd', REPLICATION_SCOPE => '1'}
hbase> add_peer 'rocketmq-repl', ENDPOINT_CLASSNAME => 'org.apache.rocketmq.hbase.Replicator'
hbase> put 'test', 'r1', 'd', 'value'
```
