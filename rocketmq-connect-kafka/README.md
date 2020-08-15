**rocketmq-connect-kafka**

在启动runtime之后，通过发送http消息到runtime，携带connector和task的参数，启动connector

**参数说明**

- **connector-class**: connector的类名
- **oms-driver-url**: rocketmq地址
- **source-record-converter**: source record converter
- **tasks.num**: 启动的task数目
- **kafka.topics**: topic列表，多个topic通过逗号“,”隔开
- **kafka.group.id**: 消费组名，多个connector中，需要保证topic和groupid的一致性
- **kafka.bootstrap.server**: kafka地址


**启动Connector**

http://127.0.0.1:8081/connectors/connector-name?config={"connector-class":"org.apache.rocketmq.connect.kafka.connector.KafkaSourceConnector","tasks.num":"1","kafka.topics":"test1,test2","kafka.group.id":"group0","kafka.bootstrap.server":"127.0.0.1:9092","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}

**查看Connector运行状态**

http://127.0.0.1:8081/connectors/connector-name/status

**查看Connector配置**

http://127.0.0.1:8081/connectors/connector-name/config

**关闭Connector**

http://127.0.0.1:8081/connectors/connector-name/stop
