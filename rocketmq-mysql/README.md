# RocketMQ-MySQL


## Overview
![overview](./doc/overview.png)

The RocketMQ-MySQL is a data replicator between MySQL and other systems, 
the replicator simulates a MySQL slave instance and parses the binlog event 
and sends it to the RocketMQ in json format ,other systems can consume data from RocketMQ. 
With the RocketMQ-MySQL Replicator,more systems can process data from MySQL binlog 
in a simple and low cost method.

## Dataflow
![dataflow](./doc/dataflow.png)

* 1.Firstly,get the last data from the queue,and get the binlog position from this data,and if the queue data is null,then use the latest binlog position of the MySQL,and surely user can also specify this position on his own;
* 2.Send a binlog dump request to the MySQL;
* 3.The MySQL push binlog event to the replicator,the replicator parses the data and accumulate as a transaction-object;
* 4.Add the next-position of the transaction to the transaction-object and send it in json format to the queue;
* 5.Record the binlog position and the offset in the queue of the latest transaction every second.


## Quick Start

* 1.Create an account with MySQL replication permission,which is used to simulate the MySQL slave to get the binlog event,and the replication must be in row mode;
* 2.Create a topic in the RocketMQ to store binlog events,in order to ensure that the downstream system consumes the data orderly,the topic must have only one queue;
* 3.Configure relevant information of MySQL and RocketMQ in the RocketMQ-MySQL.conf file;
* 4.Execute"mvn install",and then start the replicator(execute "nohup ./start.sh &");
* 5.Subscribe to and process the messages in your system.


## Configuration Instruction
|key               |nullable|default    |description|
|------------------|--------|-----------|-----------|
|mysqlAddr         |false   |           |MySQL address|
|mysqlPort         |false   |           |MySQL port|
|mysqlUsername     |false   |           |username of MySQL account|
|mysqlPassword     |false   |           |password of MySQL account|
|mqNamesrvAddr     |false   |           |RocketMQ name server address (e.g.,127.0.0.1:9876)|
|mqTopic           |false   |           |RocketMQ topic name|
|startType         |true    |NEW_EVENT  |The way that the replicator starts processing data,there are three options available:<br>- NEW_EVENT： starts processing data from the tail of binlog<br>- LAST_PROCESSED: starts processing data from the last processed event<br>- SPECIFIED:starts processing data from the position that user specified,if you choose this option,the binlogFilename and nextPosition must not be null|
|binlogFilename    |true    |           |If "startType" is "SPECIFIED",the replicator will begin to replicate from this binlog file|
|nextPosition      |true    |           |If "startType" is "SPECIFIED",the replicator will begin to replicate from this position|
|maxTransactionRows|true    |100        |max rows of the transaction pushed to RocketMQ|
=======
The RocketMQ-MySQL is a data replicator between MySQL and other system,the replicator parse the binglog event and send it in json format to the RocketMQ,and other system can pull data from RocketMQ.
## Dataflow
![dataflow](./doc/dataflow.png)

* 1.Firstly,get the last data from the queue,and get the binglog position from this data,and if the queue data is null,then use the latest binglog position of the MySQL,and surely user can also specify this position on his own;
* 2.Send a binlog dump request to the MySQL;
* 3.The MySQL push binglog event to the Replicator,the Replicator parse the data and accumulate as a transaction-object;
* 4.Add the next-position of the transaction to the transaction-object and send it in json format to the queue;
* 5.Record the binglog position and the offset in the queue of the latest transaction every second.
