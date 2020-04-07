---
description: OpenMessaging Runtime 的概念模型与设计
---

# OpenMessaging Runtime

### OpenMessaging Runtime

{% hint style="success" %}
**OpenMessaging Runtime**为其上运行的各种类型Connector及其关联的Task任务**提供统一的运行时环境**（包括**负载均衡，实例间的调度，配置管理以及集群管理**）。 因此，在RocketMQ的消息路由的特性设计与实现中，依然不会将涉及消息复制和同步的逻辑渗透至OpenMessaging Runtime层，**该层仍然是一个具体Connector实现无关且高度抽象统一的中间层。**这里，只需要将runtime 组件进行一定的参数可配置化改造即可。
{% endhint %}

![OpenMessaging Connector Runtime &#x6A21;&#x578B;](https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/connector/c2.png)

### **OpenMessaging 与消息队列的关系**

**OpenMessaging** 是一套消息中间件领域的规范。**OpenMessaging Connect**是Connect方面的**api**，**OpenMessaging** 还有其他各种消息中间件邻域的规范，例如mq客户端规范，存储规范，实现OpenMessaging 规范可以做到厂商无关**。**

 ****Connect规范的具体实现，实现这套规范的好处就是，只要是同样实现了OpenMessaging Connect的Source Connector或者Sink Connector都可以被RocketMQ Connect等 Runtime加载运行。  


### 1. Runtime 链接MQ集群的 mqDriverUrl可配置化

对于 **`OpenMessaging Runtime`**来说，其链接的MQ集群逻辑上依然是三个，可以通过以下的配置参数来加载mqDriverUrl的驱动地址，这里举例来说 mqDriverUrl如下：

{% code title="配置文件" %}
```java
a.source.rocketmq.server=xxx.xx:9876
b.runtime.rocketmq.server=xxx.xx:9876
c.sink.rocketmq.server=xxx.xx:9876
```
{% endcode %}

### 2. 利用Source/Target的RocketMQ集群保存待复制同步的中间数据

为了减少第三方的MQ集群部署增加的复杂性，这里可以借助 Source或者Target的RocketMQ集群来保存待复制同步的中间数据，这样的设计既可以实现SourceTask和Sink Task的解耦，同样可以保证Runtime组件的独立性。

{% hint style="info" %}
#### 缺点

在中间多了一次消息数据存储至MQ集群并拉取再转发至Target MQ集群
{% endhint %}



