##### ActiveConnector完全限定名
org.apache.rocketmq.connect.activemq.connector.ActivemqConnector


##### 配置参数

参数 | 作用 | 是否必填 | 默认值
---|--- |--- | ---
activemq.url | activemq ip与端口号 | 是 | 无
activemq.username | 用户名 | 否 |  无
activemq.password|  密码    | 否  | 无
jms.destination.name | 读取的队列或者主题名   |  是 | 无
jms.destination.type | 读取的类型：queue(队列)或者topic(主题) | 是 | 无
jms.message.selector | 过滤器    |  否  |无
jms.session.acknowledge.mode | 消息确认  | 否 | Session.AUTO_ACKNOWLEDGE
jms.session.transacted | 是否是事务会话      | 否 | false
