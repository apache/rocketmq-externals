# RocketMQ-connect-redis
##### RedisSourceConnector 完全限定名
org.apache.rocketmq.connect.redis.connector.RedisSourceConnector


##### 配置参数

参数 | 作用 | 是否必填 | 默认值
---|--- |--- | ---
redisAddr | Redis服务IP地址。 | 是 | null
redisPort | Redis服务端口。 | 是 | null
redisPassword | Redis auth 密码。 | 是 | null
timeout | 连接Redis超时时间，单位毫秒。 | 否 | 3000
syncMod | 数据同步模式。 | 否 | CUSTOM_OFFSET
offset | Redis位点信息。 | 否 | -1 
replId | Redis master_replId，可用redis-cli通过info命令获取。 | 否  | null 
commands | 需要监听的Redis操作命令。  | 否 | *
eventCommitRetryTimes | 收到Redis event后提交到RedisEventProcessor的失败重试次数。 | 否 | 5
eventCommitRetryInterval | 收到Redis event后提交到RedisEventProcessor的失败重试的时间间隔，单位毫秒。 | 否 | 100
