# rocketmq-spring-boot-starter-sample

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

It's a demo project for how to use [spring-boot-starter-rocketmq](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-spring-boot-starter) 

> Note:
>
> If `org.apache.rocketmq:spring-boot-starter-rocketmq:1.0.0-SNAPSHOT` not exists in maven central repository, before you run this demo, you must download [spring-boot-starter-rocketmq](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-spring-boot-starter) and use `mvn clean install` build it by self.

Run the test case locally
1. build and install the spring-boot-start-rocketmq

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
java -jar target/rocketmq-produce-demo-1.0.0-SNAPSHOT.jar

# open another terminal, run consume
cd rocketmq-consume-demo
mvn clean package
java -jar target/rocketmq-consume-demo-1.0.0-SNAPSHOT.jar
```
