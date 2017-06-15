# RocketMQ Externals

There are some RocketMQ external projects, with the purpose of growing the RocketMQ community.

## RocketMQ-Console
A newly designed RocketMQ console.

## RocketMQ-JMS
RocketMQ-JMS is an implement of JMS 1.1 specification.

## RocketMQ-Flume

This project is used to receive and send messages between
[RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume)

1. Firstly, please get familiar with [RocketMQ](http://rocketmq.incubator.apache.org/) and [Flume-ng](https://github.com/apache/flume).
2. Ensure that the jar related to [RocketMQ](http://rocketmq.incubator.apache.org/dowloading/releases) exists in local maven repository.
3. Execute the following command in rocketmq-flume root directory

   `mvn clean install dependency:copy-dependencies`

4. Copy the jar depended by rocketmq-flume to `$FLUME_HOME/lib`(the specific jar will be given later)


## RocketMQ-Spark

Apache Spark-Streaming integration with RocketMQ. Both push & pull consumer mode are provided.
For more details please refer to rocketmq-spark README.md.

## RocketMQ-Docker
Apache RocketMQ Docker provides Dockerfile and bash scripts for building and running docker image.

## RocketMQ-MySQL
This project is a data replicator between MySQL and other systems.For more details please refer to rocketmq-mysql README.md.

## Others
[RocketMQ-Ignite](https://github.com/apache/ignite/tree/master/modules/rocketmq) and [RocketMQ-Storm](https://github.com/apache/storm/tree/master/external/storm-rocketmq) integration can be found in respective repositories.
