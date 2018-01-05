# RocketMQ-Serializer
RocketMQ-Serializer is a RocketMQ extend library for serializing and deserializing message body.
Both APIs and implements(string, json, avro...) are included in this module.

## APIs
The core serializer & deserializer API are interfaces `RocketMQSerializer` and `RocketMQDeserializer`.  
In order to centralized manage avro schemas, you can implement `SchemaRegistry` interface in `rocketmq-serializer-avro` module,
and use `SchemaRegistry` registering and getting schemas.

## Implementations
### Supported Formats

| Format        | Serializer           | Deserializer  |
| ------------- |:-------------:|:------:|
| Raw String      | Y | Y |
| JSON      | Y      |   Y |
| Avro Generic | Y      |    Y |
| Avro Specified | Y      |    Y |

Some serializer performance research please refer to https://github.com/vongosling/jvm-serializer.

### User Defined Formats
You can define your format just implements `RocketMQSerializer` and `RocketMQDeserializer`.

## Tools
`Messages` provides methods like `newMessage` and `getMessageBody` to map between user class and byte array.  
`AvroUtils` provides methods `newGenericRecord` and `newSchema` to create avro records and schemas.

## Examples
### Producer Example
```
DefaultMQProducer producer = new DefaultMQProducer("producer-group-json");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        // creating serializer for message body serializing
        RocketMQSerializer serializer = new RocketMQJsonSerializer<User>();

        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setName("tom");
            user.setAge(i);

            // creating message from user data.
            Message message = Messages.newMessage("topic-json", user, serializer);
            SendResult result = producer.send(message);
            System.out.print(result.getSendStatus() + " " + i + "\n");

            Thread.sleep(1000);
        }
```

### Consumer Example
```
DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer-group-json");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("topic-json", "*");

        // creating deserializer for message body deserializing
        RocketMQDeserializer deserializer = new RocketMQJsonDeserializer<>(User.class);

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list,
                                                            ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                for (MessageExt messageExt : list) {
                    // getting data from message.
                    User user = Messages.getMessageBody(messageExt, deserializer);
                    System.out.print(user.getName() + ":" + user.getAge() + "\n");
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
```

## Internals
`rocketmq-serializer-avro` is powered by Apache Avro, and `rocketmq-serializer-json` is powered by fastjson.

