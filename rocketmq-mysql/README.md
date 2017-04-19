# RocketMQ-MySQL


## Overview
![overview](./doc/overview.png)

The RocketMQ-MySQL is a data replicator between MySQL and other system,the replicator parse the binglog event and send it in json format to the RocketMQ,and other system can pull data from RocketMQ.
## Dataflow
![dataflow](./doc/dataflow.png)

* 1.Firstly,get the last data from the queue,and get the binglog position from this data,and if the queue data is null,then use the latest binglog position of the MySQL,and surely user can also specify this position on his own;
* 2.Send a binlog dump request to the MySQL;
* 3.The MySQL push binglog event to the Replicator,the Replicator parse the data and accumulate as a transaction-object;
* 4.Add the next-position of the transaction to the transaction-object and send it in json format to the queue;
* 5.Record the binglog position and the offset in the queue of the latest transaction every second.