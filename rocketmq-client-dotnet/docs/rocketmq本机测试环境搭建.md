# RocketMQ 调试环境搭建

## 依赖工具

* JDK ：1.8+
* Maven 
* IntelliJ IDEA

## 源码拉取

使用git获取源码

```bash
    git clone  https://github.com/apache/rocketmq.git
```

## 启动服务

### 服务总览

1. 启动`RocektMQ Namesrv`
2. 启动`RocektMQ Broker`

* 可选测试
    1. `启动Producer`
    2. `启动Consumer`
 
### 启动  RocketMQ Namesrv

1. 使用IDEA打开项目 `rocketmq\namesrv`
2. 打开 `org.apache.rocketmq.namesrv.NameServerInstanceTest` 单元测试类，参考 `#startup()` 方法，编写 `#main(String[] args)` 静态方法，代码如下：

```java

// NameServerInstanceTest.java

public static void main(String[] args) throws Exception {
    // NamesrvConfig 配置
    final NamesrvConfig namesrvConfig = new NamesrvConfig();
    // NettyServerConfig 配置
    final NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(9876); // 设置端口
    // 创建 NamesrvController 对象，并启动
    NamesrvController namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
    namesrvController.initialize();
    namesrvController.start();
    // 睡觉，就不起来
    Thread.sleep(DateUtils.MILLIS_PER_DAY);
}

```
3. 右键运行该方法
   
4. 查看控制台日志如下即说明启动完成

``` 
17:54:03.354 [NettyEventExecutor] INFO  RocketmqRemoting - NettyEventExecutor service started
17:54:03.355 [FileWatchService] INFO  RocketmqCommon - FileWatchService service started
```

### 启动 RocketMQ Broker

1. 使用IDEA 在新窗口打开项目`rocketmq\broker`
2. 打开 `org.apache.rocketmq.broker.BrokerControllerTest` 单元测试类，参考 `testBrokerRestart()` 方法，编写 `main(String[] args)`
```JAVA

// BrokerControllerTest.java

public static void main(String[] args) throws Exception {
    // 设置版本号
    System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
    // NettyServerConfig 配置
    final NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(10911);
    // BrokerConfig 配置
    final BrokerConfig brokerConfig = new BrokerConfig();
    brokerConfig.setBrokerName("broker-a");
    brokerConfig.setNamesrvAddr("127.0.0.1:9876");
    // MessageStoreConfig 配置
    final MessageStoreConfig messageStoreConfig = new MessageStoreConfig();
    messageStoreConfig.setDeleteWhen("04");
    messageStoreConfig.setFileReservedTime(48);
    messageStoreConfig.setFlushDiskType(FlushDiskType.ASYNC_FLUSH);
    messageStoreConfig.setDuplicationEnable(false);

//        BrokerPathConfigHelper.setBrokerConfigPath("/Users/yunai/百度云同步盘/开发/Javascript/Story/incubator-rocketmq/conf/broker.conf");
    // 创建 BrokerController 对象，并启动
    BrokerController brokerController = new BrokerController(//
            brokerConfig, //
            nettyServerConfig, //
            new NettyClientConfig(), //
            messageStoreConfig);
    brokerController.initialize();
    brokerController.start();
    // 睡觉，就不起来
    System.out.println("启动Broker");
    Thread.sleep(DateUtils.MILLIS_PER_DAY);
}

```

3. 在`namesrv`的控制台看到如下信息即可成功

```
340 [RemotingExecutorThread_3] DEBUG RocketmqNamesrv - receive request, 103 127.0.0.1:8706 RemotingCommand [code=103, language=JAVA, version=315, opaque=4848, flag(B)=0, remark=null, extFields={brokerId=0, bodyCrc32=1939801840, clusterName=DefaultCluster, brokerAddr=192.168.190.108:10911, haServerAddr=192.168.190.108:10912, compressed=false, brokerName=broker-a}, serializeTypeCurrentRPC=JSON]
21:10:31.313 [RemotingExecutorThread_4] DEBUG RocketmqNamesrv - receive request, 103 127.0.0.1:8706 RemotingCommand [code=103, language=JAVA, version=315, opaque=4850, flag(B)=0, remark=null, extFields={brokerId=0, bodyCrc32=1939801840, clusterName=DefaultCluster, brokerAddr=192.168.190.108:10911, haServerAddr=192.168.190.108:10912, compressed=false, brokerName=broker-a}, serializeTypeCurrentRPC=JSON]
```

## 可选任务

### 启动RocketMQ Producer

1. 打开 org.apache.rocketmq.example.quickstart.Producer 示例类，
2. 增加一行代码
   
```JAVA
  producer.setNamesrvAddr("127.0.0.1:9876"); // 添加nameserver 地址
```

3. 完整代码
```JAVA

// Producer.java

/**
 * This class demonstrates how to send messages to brokers using provided {@link DefaultMQProducer}.
 */
public class Producer {

    public static void main(String[] args) throws MQClientException, InterruptedException {

        /*
         * Instantiate with a producer group name.
         */
        DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");

        /*
         * Specify name server addresses.
         * <p/>
         *
         * Alternatively, you may specify name server addresses via exporting environmental variable: NAMESRV_ADDR
         * <pre>
         * {@code
         * producer.setNamesrvAddr("name-server1-ip:9876;name-server2-ip:9876");
         * }
         * </pre>
         */

        /*
         * Launch the instance.
         */
        producer.setNamesrvAddr("127.0.0.1:9876"); // 添加nameserver 地址
        producer.start();

        for (int i = 0; i < 1000; i++) {
            try {

                /*
                 * Create a message instance, specifying topic, tag and message body.
                 */
                Message msg = new Message("TopicTest" /* Topic */,
                    "TagA" /* Tag */,
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
                );

                /*
                 * Call send message to deliver message to one of brokers.
                 */
                SendResult sendResult = producer.send(msg);

                System.out.printf("%s%n", sendResult);
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(1000);
            }
        }

        /*
         * Shut down once the producer instance is not longer in use.
         */
        producer.shutdown();
    }
}
 
```

4. 控制台日志

```

18:22:13.507 [main] DEBUG i.n.u.i.l.InternalLoggerFactory - Using SLF4J as the default logging framework
SendResult [sendStatus=SEND_OK, msgId=C0A8031AE91718B4AAC27A6364050000, offsetMsgId=C0A8031A00002A9F0000000000000000, messageQueue=MessageQueue [topic=TopicTest, brokerName=broker-a, queueId=1], queueOffset=0]

··· 省略掉
```


### 启动RocketMQ Consumer

1. 打开 `org.apache.rocketmq.example.quickstart.Consumer` 示例类
2. 添加 `nameserver` 地址

```JAVA

    consumer.setNamesrvAddr("127.0.0.1:9876"); // 添加nameserver 地址 
```

* 完整代码

```JAVA

// Consumer.java

public class Consumer {

    public static void main(String[] args) throws InterruptedException, MQClientException {

        /*
         * Instantiate with specified consumer group name.
         */
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name_4");

        /*
         * Specify name server addresses.
         * <p/>
         *
         * Alternatively, you may specify name server addresses via exporting environmental variable: NAMESRV_ADDR
         * <pre>
         * {@code
         * consumer.setNamesrvAddr("name-server1-ip:9876;name-server2-ip:9876");
         * }
         * </pre>
         */

        /*
         * Specify where to start in case the specified consumer group is a brand new one.
         */
        consumer.setNamesrvAddr("127.0.0.1:9876"); //设置namesrver 地址

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        /*
         * Subscribe one more more topics to consume.
         */
        consumer.subscribe("TopicTest", "*");

        /*
         *  Register callback to execute on arrival of messages fetched from brokers.
         */
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        /*
         *  Launch the consumer instance.
         */
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }

}

```

3. 控制台日志

```

18:37:12.196 [main] DEBUG i.n.u.i.l.InternalLoggerFactory - Using SLF4J as the default logging framework
Consumer Started.
ConsumeMessageThread_2 Receive New Messages: [MessageExt [queueId=3, storeSize=178, queueOffset=0, sysFlag=0, bornTimestamp=1543054934061, bornHost=/192.168.3.26:64103, storeTimestamp=1543054934065, storeHost=/192.168.3.26:10911, msgId=C0A8031A00002A9F0000000000000164, commitLogOffset=356, bodyCRC=1250039395, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='TopicTest', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=250, CONSUME_START_TIME=1543055832771, UNIQ_KEY=C0A8031AE91718B4AAC27A63642D0002, WAIT=true, TAGS=TagA}, body=[72, 101, 108, 108, 111, 32, 82, 111, 99, 107, 101, 116, 77, 81, 32, 50], transactionId='null'}]] 
ConsumeMessageThread_16 Receive New Messages: [MessageExt [queueId=2, storeSize=179, queueOffset=4, sysFlag=0, bornTimestamp=1543054934102, bornHost=/192.168.3.26:64103, storeTimestamp=1543054934103, storeHost=/192.168.3.26:10911, msgId=C0A8031A00002A9F0000000000000BD9, commitLogOffset=30

```
