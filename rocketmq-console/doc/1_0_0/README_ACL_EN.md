# RocketMQ Console ACL USES the documentation

RocketMQ 4.4.0 starts to support ACL feature, and if your MQ broker has ACL enabled, then usually the console has to have this enabled as well.
Otherwise, actions such as creating topic and updating topic cannot be performed in the console.

Configure in application.properties:

``` $XSLT
The acl function on the console is enabled. It is not enabled by default.
Rocketmq. Config. AclEnable = true

# if the acl function of the console is enabled, then the admin ak and sk configured by MQ broker must be set.
The following is rocketMQ broker's default ak and sk for admin.
Rocketmq. Config. The accessKey = rocketmq2
Rocketmq. Config. SecretKey = 1234567
```