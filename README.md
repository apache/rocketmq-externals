# RocketMQ Externals

There are some RocketMQ external projects, with the purpose of growing the RocketMQ community.

## RocketMQ-Console-Ng
A console for RocketMQ
## RocketMQ-JMS
RocketMQ-JMS is an implement of JMS specification,taking Apache RocketMQ as broker. Now we are on the way of supporting JMS 1.1 and JMS2.0 is our final target.

## RocketMQ-Flume-Ng

This project is used to receive and send messages between
[RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume)

1. Firstly, please get familiar with [RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume).
2. Ensure that the jar related to [RocketMQ](http://rocketmq.incubator.apache.org/dowloading/releases) exists in local maven repository.
3. Execute the following command in rocketmq-flume root directory

   `mvn clean install dependency:copy-dependencies`

4. Copy the jar depended by rocketmq-flume to `$FLUME_HOME/lib`(the specific jar will be given later)

## RocketMQ-Spark-Streaming

Apache Spark-Streaming integration with RocketMQ. Two Receiver provided:
* RocketMQReceiver - which is no fault-tolerance guarantees
* ReliableRocketMQReceiver - which is fault-tolerance guarantees
