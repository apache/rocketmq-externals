---
description: rocketmq的管理工具使用指南
---

# mqadmin 操作指南

## mqadmin管理工具

{% hint style="warning" %}
RocketMQ 功能复杂，本篇只说明一些RocketMQ Connect常用命令，更多细节参考[RocketMQ官网](https://rocketmq.apache.org/)
{% endhint %}

在`{rocketmq目录}/bin`中有**mqadmin管理工具**

> #### 注意：
>
> 1. 执行命令方法：`./mqadmin {command} {args}`
> 2. 几乎所有命令都需要配置-n表示NameServer地址，格式为ip:port
> 3. 几乎所有命令都可以通过-h获取帮助
> 4. 如果既有Broker地址（-b）配置项又有clusterName（-c）配置项，则优先以Broker地址执行命令，如果不配置Broker地址，则对集群中所有主机执行命令，只支持一个Broker地址。-b格式为ip:port，port默认是10911
> 5. 在tools下可以看到很多命令，但并不是所有命令都能使用，只有在MQAdminStartup中初始化的命令才能使用，你也可以修改这个类，增加或自定义命令
> 6. 由于版本更新问题，少部分命令可能未及时更新，遇到错误请直接阅读相关命令源码

### **Topic相关**

| 名称 | 含义 | 命令选项 | 说明 |
| :--- | :--- | :--- | :--- |
| updateTopic | 创建更新Topic配置 | -b | Broker 地址，表示 topic 所在 Broker，只支持单台Broker，地址为ip:port |
| -c | cluster 名称，表示 topic 所在集群（集群可通过 clusterList 查询） |  |  |
| -h- | 打印帮助 |  |  |
| -n | NameServer服务地址，格式 ip:port |  |  |
| -p | 指定新topic的读写权限\( W=2\|R=4\|WR=6 \) |  |  |
| -r | 可读队列数（默认为 8） |  |  |
| -w | 可写队列数（默认为 8） |  |  |
| -t | topic 名称（名称只能使用字符 ^\[a-zA-Z0-9\_-\]+$ ） |  |  |
| deleteTopic | 删除Topic | -c | cluster 名称，表示删除某集群下的某个 topic （集群 可通过 clusterList 查询） |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -t | topic 名称（名称只能使用字符 ^\[a-zA-Z0-9\_-\]+$ ） |  |  |
| topicList | 查看 Topic 列表信息 | -h | 打印帮助 |
| -c | 不配置-c只返回topic列表，增加-c返回clusterName, topic, consumerGroup信息，即topic的所属集群和订阅关系，没有参数 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| topicRoute | 查看 Topic 路由信息 | -t | topic 名称 |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| topicStatus | 查看 Topic 消息队列offset | -t | topic 名称 |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| topicClusterList | 查看 Topic 所在集群列表 | -t | topic 名称 |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| updateTopicPerm | 更新 Topic 读写权限 | -t | topic 名称 |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -b | Broker 地址，表示 topic 所在 Broker，只支持单台Broker，地址为ip:port |  |  |
| -p | 指定新 topic 的读写权限\( W=2\|R=4\|WR=6 \) |  |  |
| -c | cluster 名称，表示 topic 所在集群（集群可通过 clusterList 查询），-b优先，如果没有-b，则对集群中所有Broker执行命令 |  |  |
| updateOrderConf | 从NameServer上创建、删除、获取特定命名空间的kv配置，目前还未启用 | -h | 打印帮助 |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -t | topic，键 |  |  |
| -v | orderConf，值 |  |  |
| -m | method，可选get、put、delete |  |  |
| allocateMQ | 以平均负载算法计算消费者列表负载消息队列的负载结果 | -t | topic 名称 |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -i | ipList，用逗号分隔，计算这些ip去负载Topic的消息队列 |  |  |
| statsAll | 打印Topic订阅关系、TPS、积累量、24h读写总量等信息 | -h | 打印帮助 |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -a | 是否只打印活跃topic |  |  |
| -t | 指定topic |  |  |

### **集群相关**

| 名称 | 含义 | 命令选项 | 说明 |
| :--- | :--- | :--- | :--- |
| clusterList | 查看集群信息，集群、BrokerName、BrokerId、TPS等信息 | -m | 打印更多信息 \(增加打印出如下信息 \#InTotalYest, \#OutTotalYest, \#InTotalToday ,\#OutTotalToday\) |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -i | 打印间隔，单位秒 |  |  |
| clusterRT | 发送消息检测集群各Broker RT。消息发往${BrokerName} Topic。 | -a | amount，每次探测的总数，RT = 总时间 / amount |
| -s | 消息大小，单位B |  |  |
| -c | 探测哪个集群 |  |  |
| -p | 是否打印格式化日志，以\|分割，默认不打印 |  |  |
| -h | 打印帮助 |  |  |
| -m | 所属机房，打印使用 |  |  |
| -i | 发送间隔，单位秒 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |

### **Broker相关**

| 名称 | 含义 | 命令选项 | 说明 |
| :--- | :--- | :--- | :--- |
| updateBrokerConfig | 更新 Broker 配置文件，会修改Broker.conf | -b | Broker 地址，格式为ip:port |
| -c | cluster 名称 |  |  |
| -k | key 值 |  |  |
| -v | value 值 |  |  |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| brokerStatus | 查看 Broker 统计信息、运行状态（你想要的信息几乎都在里面） | -b | Broker 地址，地址为ip:port |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| brokerConsumeStats | Broker中各个消费者的消费情况，按Message Queue维度返回Consume Offset，Broker Offset，Diff，TImestamp等信息 | -b | Broker 地址，地址为ip:port |
| -t | 请求超时时间 |  |  |
| -l | diff阈值，超过阈值才打印 |  |  |
| -o | 是否为顺序topic，一般为false |  |  |
| -h | 打印帮助 |  |  |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| getBrokerConfig | 获取Broker配置 | -b | Broker 地址，地址为ip:port |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| wipeWritePerm | 从NameServer上清除 Broker写权限 | -b | Broker 地址，地址为ip:port |
| -n | NameServer 服务地址，格式 ip:port |  |  |
| -h | 打印帮助 |  |  |
| cleanExpiredCQ | 清理Broker上过期的Consume Queue，如果手动减少对列数可能产生过期队列 | -n | NameServer 服务地址，格式 ip:port |
| -h | 打印帮助 |  |  |
| -b | Broker 地址，地址为ip:port |  |  |
| -c | 集群名称 |  |  |
| cleanUnusedTopic | 清理Broker上不使用的Topic，从内存中释放Topic的Consume Queue，如果手动删除Topic会产生不使用的Topic | -n | NameServer 服务地址，格式 ip:port |
| -h | 打印帮助 |  |  |
| -b | Broker 地址，地址为ip:port |  |  |
| -c | 集群名称 |  |  |
| sendMsgStatus | 向Broker发消息，返回发送状态和RT | -n | NameServer 服务地址，格式 ip:port |
| -h | 打印帮助 |  |  |
| -b | BrokerName，注意不同于Broker地址 |  |  |
| -s | 消息大小，单位B |  |  |
| -c | 发送次数 |  |  |

### 更多详细信息请参考

### \*\*\*\*[**运维管理**](https://github.com/apache/rocketmq/blob/master/docs/cn/operation.md)\*\*\*\*

