# RocketMQ Console ACL 使用文档

RocketMQ 4.4.0 开始支持 ACL 功能, 如果你的 MQ broker 开启了 ACL 功能, 那么,通常 console 也得开启此功能.
否则, 无法在 console 执行创建 topic 和更新 topic 等操作. 

在 application.properties 中配置:

```$xslt
# true 表示开启控制台的 acl 功能. 默认不开启.
rocketmq.config.aclEnable=true

# 如果开启了控制台的 acl 功能, 那么就必须设置 MQ broker 配置的 admin 的 ak 和 sk.
# 以下是 rocketMQ broker 的默认 admin 的 ak 和 sk.
rocketmq.config.accessKey=rocketmq2
rocketmq.config.secretKey=1234567
```

