# rocketmq-replicator

# 启动参数选择

参数 | 类型 |是否必须 |说明|示例值
---|---|---|---|---|
source-rocketmq | 字符串 | 是 | 源rocketmq集群namesrv地址 | 192.168.1.2:9876 |
target-rocketmq | 字符串 | 是 | 源rocketmq集群namesrv地址 | 192.168.1.2:9876 |
replicator-store-topic | 字符串 | 是 | replicator存储topic，需要在runtime的mq集群提前创建 | replicator-store-topic |
task-divide-strategy | 整型 | 否 | 任务切割策略，可以按照主题和队列来切割，目前只支持主题切割且主题对应值为0 | 0 |
white-list | 字符串 | 是 | 复制主题白名单，多个topic之间使用逗号分隔 | topic-1,topic-2 |
task-parallelism | 整型 | 否 | 任务并行度，默认值为1，当topic数大于task数时，一个task将负责多个topic | 2 |
source-record-converter | 字符串 | 是 | 源数据解析器，目前使用的是Json解析器 | io.openmessaging.connect.runtime.converter.JsonConverter |