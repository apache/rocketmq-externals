---
description: 运行Connector实例和Task的线程，一个Worker进程代表来一个Connector Runtime 运行时环境进程
---

# Woker

{% hint style="success" %}
#### **Worker的定义**

运行Connector实例和Task的线程，一个Worker进程代表来一个Connector Runtime 运行时环境进程，多个Worker进程组成了一个集群，支持更多的Connect 和 Task的并行运行工作。其中，Connector Runtime 还具备了**配置管理、负载均衡和任务调度。**
{% endhint %}

![Worker &#x8FDB;&#x7A0B;&#x6A21;&#x578B;](../.gitbook/assets/c2.png)

### Worker单实例

* 若干个Connector实例以及相关联的Task任务都运行在同一个Worker实例进程上，这样子不需要调度和负载均衡，整个结构比较简单。
* 缺点也明显，消息路由的弹性扩展，消息同步的吞吐量、可容错性都比较差。

### Worker 分布式集群

![Worker &#x5206;&#x5E03;&#x5F0F;&#x96C6;&#x7FA4;&#x6A21;&#x578B;](../.gitbook/assets/c1%20%281%29.png)

若干个Connector实例以及相关联的Task任务会运行在不同的Worker实例进程上，这样子需要考虑好**调度和负载均衡**。在该模式中，**弹性扩展、吞吐量和可容错性**都比[Worker单实例](woker-shi-li-mo-xing.md#worker-dan-shi-li)要提升很多。

