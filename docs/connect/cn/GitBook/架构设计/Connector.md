---
description: 连接器实例属于逻辑概念，其负责维护特定数据系统的相关配置，比如链接地址、需要同步哪些数据等信息。
---

# Message Connector



{% hint style="success" %}
#### **Message Connector**

连接器实例属于逻辑概念，其负责维护特定数据系统的相关配置，比如链接地址、需要同步哪些数据等信息；在connector 实例被启动后，connector可以根据配置信息，对解析任务进行拆分，分配出task。这么做的目的是为了提高并行度，提升处理效率。
{% endhint %}

![Connector &#x6982;&#x5FF5;&#x56FE;](../.gitbook/assets/c1.png)

### Connector 两个重要概念

*  **Connector**

  Connector的入口，**负责管理Task的配置和创建**，TaskClass返回实际要创建的Task，通过TaskConfigs返回Task的配置信息，这个返回的配置是一个list，创建Task的数量就是根据返回的config列表的数量。

* **Task**

        ****解析数据源数据，并负责在数据源和RocketMQ Broker之间拷贝数据

### **Connector 示例**

* **[**rocketmq-connector**](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-connect)**
* **[**rocketmq-mysql**](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-mysql)**
* **[**rocketmq-connect-rabbitmq**](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-connect-rabbitmq)**
* **[**rocketmq-connect-kafka**](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-connect-kafka)**
* **[**rocketmq-connect-activemq**](https://github.com/apache/rocketmq-externals/tree/master/rocketmq-connect-activemq)**

### **Quick Start**

{% page-ref page="../quick-start/前期准备/" %}





