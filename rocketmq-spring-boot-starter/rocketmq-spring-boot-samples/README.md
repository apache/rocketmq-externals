# rocketmq-spring-boot-samples

[中文](./README_zh_CN.md)

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

It's a demo project for how to use [rocketmq-spring-boot](https://github.com/apache/rocketmq-spring)

> Note:
>
> If `org.apache.rocketmq:rocketmq-spring-boot-starter:2.0.0-SNAPSHOT` not exists in maven central repository, before you run this demo, you must git clone [rocketmq-spring-boot](https://github.com/apache/rocketmq-spring) and use `mvn clean install` build it by yourself.

Run the test case locally
1. build and install the rocketmq-spring-boot-starter

2. startup rocketmq according to quick-start, verify the namesvr and broker startup correctly, Note: DON'T do "Shutdown Servers" step.
http://rocketmq.apache.org/docs/quick-start/

3. create topics for the demo test cases
```
bash bin/mqadmin updateTopic -c DefaultCluster -t string-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t order-paid-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t message-ext-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t spring-transaction-topic
```
4. run tests

```
# open a terminal, run produce
cd rocketmq-produce-demo
mvn clean package
java -jar target/rocketmq-produce-demo-0.0.1-SNAPSHOT.jar

# open another terminal, run consume
cd rocketmq-consume-demo
mvn clean package
java -jar target/rocketmq-consume-demo-0.0.1-SNAPSHOT.jar
```
