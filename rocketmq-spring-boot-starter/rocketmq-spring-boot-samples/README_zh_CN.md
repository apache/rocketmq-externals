# rocketmq-spring-boot-samples

[English](./README.md)

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

这里是一个使用rocketmq-spring-boot-starter的列子。 [rocketmq-spring-boot](https://github.com/apache/rocketmq-spring)

> 注意:
>
> 如果这个dependency: `org.apache.rocketmq:rocketmq-spring-boot-starter:2.0.0-SNAPSHOT`在maven中心库不存在, 需要开发者在本地build并安装到本地maven库，执行如下步骤:
```
 git clone https://github.com/apache/rocketmq-spring
 cd rocketmq-spring
 mvn clean install
```

## 在本地运行这个测试例子

1. 如上面注意项所述，需要开发者在本地build并安装rocketmq-spring-boot-starter

2. 根据RocketMQ官网的quick-start来启动NameServer和Broker，并验证是否启动正确。注意: 测试期间不要停止Broker或者NameServer
http://rocketmq.apache.org/docs/quick-start/

3. 创建测试例子所需要的Topic
```
cd YOUR_ROCKETMQ_HOME

bash bin/mqadmin updateTopic -c DefaultCluster -t string-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t order-paid-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t message-ext-topic
bash bin/mqadmin updateTopic -c DefaultCluster -t spring-transaction-topic
```

4. 编译并运行测试例子

```
# 打开一个终端窗口，编译并启动发送端
cd rocketmq-produce-demo
mvn clean package
java -jar target/rocketmq-produce-demo-0.0.1-SNAPSHOT.jar

# 打开另一个终端窗口，编译并启动消费端
cd rocketmq-consume-demo
mvn clean package
java -jar target/rocketmq-consume-demo-0.0.1-SNAPSHOT.jar
```
结合测试代码，观察窗口中消息的发送和接收情况
