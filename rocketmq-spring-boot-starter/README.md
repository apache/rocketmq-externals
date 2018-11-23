# spring-boot-starter-rocketmq

[中文](./README_zh_CN.md)

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Help developers quickly integrate [RocketMQ](http://rocketmq.apache.org/) in [Spring Boot](http://projects.spring.io/spring-boot/). Support the Spring Message specification to facilitate developers to quickly switch from other MQ to RocketMQ.


Features:

- [x] synchronous transmission
- [x] synchronous ordered transmission
- [x] asynchronous transmission
- [x] asynchronous ordered transmission
- [x] orderly consume
- [x] concurrently consume(broadcasting/clustering)
- [x] one-way transmission
- [x] transaction transmission
- [ ] pull consume

## Quick Start

Here are some key points listed, the complete example, please refer to: [rocketmq-demo](https://github.com/aqlu/rocketmq-demo)

```xml
<!--add dependency in pom.xml-->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>spring-boot-starter-rocketmq</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Produce Message

```properties
## application.properties
spring.rocketmq.name-server=127.0.0.1:9876
spring.rocketmq.producer.group=my-group
```

> Note:
> 
> Maybe you need change `127.0.0.1:9876` with your real NameServer address for RocketMQ

```java
@SpringBootApplication
public class ProducerApplication implements CommandLineRunner{
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    public static void main(String[] args){
        SpringApplication.run(ProducerApplication.class, args);
    }
    
    public void run(String... args) throws Exception {
        rocketMQTemplate.convertAndSend("test-topic-1", "Hello, World!");
        rocketMQTemplate.send("test-topic-1", MessageBuilder.withPayload("Hello, World! I'm from spring message").build());
        rocketMQTemplate.convertAndSend("test-topic-2", new OrderPaidEvent("T_001", new BigDecimal("88.00")));
        
//        rocketMQTemplate.destroy(); // notes:  once rocketMQTemplate be destroyed, you can not send any message again with this rocketMQTemplate
    }
    
    @Data
    @AllArgsConstructor
    public class OrderPaidEvent implements Serializable{
        private String orderId;
        
        private BigDecimal paidMoney;
    }
}
```

> More relevant configurations for produce:
>
> ```properties
> spring.rocketmq.producer.retry-times-when-send-async-failed=0
> spring.rocketmq.producer.send-msg-timeout=300000
> spring.rocketmq.producer.compress-msg-body-over-howmuch=4096
> spring.rocketmq.producer.max-message-size=4194304
> spring.rocketmq.producer.retry-another-broker-when-not-store-ok=false
> spring.rocketmq.producer.retry-times-when-send-failed=2
> ```


### Send message in transaction and implement local check Listener
```java
@SpringBootApplication
public class ProducerApplication implements CommandLineRunner{
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public static void main(String[] args){
        SpringApplication.run(ProducerApplication.class, args);
    }

    public void run(String... args) throws Exception {
        try {
            // Build a SpringMessage for sending in transaction
            Message msg = MessageBuilder.withPayload(..)...
            // In sendMessageInTransaction(), the first parameter transaction name ("test")
            // must be same with the @RocketMQTransactionListener's member field 'transName'
            rocketMQTemplate.sendMessageInTransaction("test", "test-topic" msg, null);
        } catch (MQClientException e) {
            e.printStackTrace(System.out);
        }
    }

    // Define transaction listener with the annotation @RocketMQTransactionListener
    @RocketMQTransactionListener(transName="test")
    class TransactionListenerImpl implements RocketMQLocalTransactionListener() {
          @Override
          public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            // ... local transaction process
            return RocketMQLocalTransactionState.UNKNOW;
          }

          @Override
          public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
            // ... check transaction status and retun bollback or commit
            return RocketMQLocalTransactionState.COMMIT_MESSAGE;
          }
    }
}
```

### Consume Message

```properties
## application.properties
spring.rocketmq.name-server=127.0.0.1:9876
```

> Note:
> 
> Maybe you need change `127.0.0.1:9876` with your real NameServer address for RocketMQ

```java
@SpringBootApplication
public class ConsumerApplication{
    
    public static void main(String[] args){
        SpringApplication.run(ConsumerApplication.class, args);
    }
    
    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "test-topic-1", consumerGroup = "my-consumer_test-topic-1")
    public class MyConsumer1 implements RocketMQListener<String>{
        public void onMessage(String message) {
            log.info("received message: {}", message);
        }
    }
    
    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "test-topic-2", consumerGroup = "my-consumer_test-topic-2")
    public class MyConsumer2 implements RocketMQListener<OrderPaidEvent>{
        public void onMessage(OrderPaidEvent orderPaidEvent) {
            log.info("received orderPaidEvent: {}", orderPaidEvent);
        }
    }
}
```

> More relevant configurations for consume:
>
> see: [RocketMQMessageListener](src/main/java/org/apache/rocketmq/spring/starter/annotation/RocketMQMessageListener.java)


## FAQ

1. How to connected many `nameserver` on production environment？

    `spring.rocketmq.name-server` support the configuration of multiple `nameserver`, separated by `;`. For example: `172.19.0.1: 9876; 172.19.0.2: 9876`

1. When was `rocketMQTemplate` destroyed?

    Developers do not need to manually execute the `rocketMQTemplate.destroy ()` method when using `rocketMQTemplate` to send a message in the project, and` rocketMQTemplate` will be destroyed automatically when the spring container is destroyed.

1. start exception：`Caused by: org.apache.rocketmq.client.exception.MQClientException: The consumer group[xxx] has been created before, specify another name please`

   RocketMQ in the design do not want a consumer to deal with multiple types of messages at the same time, so the same `consumerGroup` consumer responsibility should be the same, do not do different things (that is, consumption of multiple topics). Suggested `consumerGroup` and` topic` one correspondence.
    
1. How is the message content body being serialized and deserialized?

    RocketMQ's message body is stored as `byte []`. When the business system message content body if it is `java.lang.String` type, unified in accordance with` utf-8` code into `byte []`; If the business system message content is not `java.lang.String` Type, then use [jackson-databind](https://github.com/FasterXML/jackson-databind) serialized into the `JSON` format string, and then unified in accordance with` utf-8` code into `byte [] `.
    
1. How do I specify the `tags` for topic?

    RocketMQ best practice recommended: an application as much as possible with one Topic, the message sub-type with `tags` to identify,` tags` can be set by the application free.
    
    When you use `rocketMQTemplate` to send a message, set the destination of the message by setting the` destination` parameter of the send method. The `destination` format is `topicName:tagName`, `:` Precedes the name of the topic, followed by the `tags` name.
    
    > Note:
    >
    > `tags` looks a complex, but when sending a message , the destination can only specify one topic under a `tag`, can not specify multiple.
    
1. How do I set the message's `key` when sending a message?

    You can send a message by overloading method like `xxxSend(String destination, Message<?> msg, ...)`, setting `headers` of `msg`. for example:
    
    ```java
    Message<?> message = MessageBuilder.withPayload(payload).setHeader(MessageConst.PROPERTY_KEYS, msgId).build();
    rocketMQTemplate.send("topic-test", message);
    ```

    Similarly, you can also set the message `FLAG`,` WAIT_STORE_MSG_OK` and some other user-defined other header information according to the above method.
    
    > Note:
    >
    > In the case of converting Spring's Message to RocketMQ's Message, to prevent the `header` information from conflicting with RocketMQ's system properties, the prefix `USERS_` was added in front of all `header` names. So if you want to get a custom message header when consuming, please pass through the key at the beginning of `USERS_` in the header.
    
1. When consume message, in addition to get the message `payload`, but also want to get RocketMQ message of other system attributes, how to do?

    Consumers in the realization of `RocketMQListener` interface, only need to be generic for the` MessageExt` can, so in the `onMessage` method will receive RocketMQ native 'MessageExt` message.
    
    ```java
    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "test-topic-1", consumerGroup = "my-consumer_test-topic-1")
    public class MyConsumer2 implements RocketMQListener<MessageExt>{
        public void onMessage(MessageExt messageExt) {
            log.info("received messageExt: {}", messageExt);
        }
    }
    ```
    
1. How do I specify where consumers start consuming messages?

    The default consume offset please refer: [RocketMQ FAQ](http://rocketmq.apache.org/docs/faq/).
    To customize the consumer's starting location, simply add a `RocketMQPushConsumerLifecycleListener` interface implementation to the consumer class. Examples are as follows:
    
    ```java
    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "test-topic-1", consumerGroup = "my-consumer_test-topic-1")
    public class MyConsumer1 implements RocketMQListener<String>, RocketMQPushConsumerLifecycleListener {
        @Override
        public void onMessage(String message) {
            log.info("received message: {}", message);
        }
    
        @Override
        public void prepareStart(final DefaultMQPushConsumer consumer) {
            // set consumer consume message from now
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
            consumer.setConsumeTimestamp(UtilAll.timeMillisToHumanString3(System.currentTimeMillis()));
        }
    }
    ```
    
    Similarly, any other configuration on `DefaultMQPushConsumer` can be done in the same way as above.


1. How do I send transactional messages?
   It needs two steps on client side: a) Define a class which is annotated with @RocketMQTransactionListener and implements RocketMQLocalTransactionListener interface, in which, the executeLocalTransaction() and checkLocalTransaction() methods are implemented;
   b) Invoke the sendMessageInTransaction() method with the RocketMQTemplate API. Note: The first parameter of this method is correlated with the txProducerGroup attribute of @RocketMQTransactionListener. It can be null if using the default transaction producer group.