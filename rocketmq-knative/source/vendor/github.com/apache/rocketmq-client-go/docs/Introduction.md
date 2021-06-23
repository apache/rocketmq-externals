## How to use

### go mod
```
require (
    github.com/apache/rocketmq-client-go v2.0.0-alpha1
)
```

### Set Logger
Go Client define the `Logger` interface for log output, user can specify implementation of private.
in default, client use `logrus`.
```go
rlog.SetLogger(Logger)
```

### Send message
#### Interface
```go
Producer interface {
	Start() error
	Shutdown() error
	SendSync(context.Context, *primitive.Message) (*internal.SendResult, error)
	SendOneWay(context.Context, *primitive.Message) error
}
```

#### Examples
- create a new `Producer` instance
```go
opt := producer.ProducerOptions{
    NameServerAddr:           "127.0.0.1:9876",
    RetryTimesWhenSendFailed: 2,
}
p := producer.NewProducer(opt)
```

- start the producer
```go 
err := p.Start()
```

- send message with sync
```go
result, err := p.SendSync(context.Background(), &primitive.Message{
    Topic: "test",
    Body:  []byte("Hello RocketMQ Go Client!"),
})

// do something with result
```

- or send message with oneway
```go 
err := p.SendOneWay(context.Background(), &primitive.Message{
    Topic: "test",
    Body:  []byte("Hello RocketMQ Go Client!"),
})
```
Full examples: [producer](../examples/producer)

### Consume Message
alpha1 only support `PushConsumer`

#### Interface
```go
PushConsumer interface {
	Start() error
	Shutdown()
	Subscribe(topic string, selector MessageSelector,
		f func(*ConsumeMessageContext, []*primitive.MessageExt) (ConsumeResult, error)) error
}
```

#### Usage
- Create a `PushConsumer` instance
```go
c := consumer.NewPushConsumer("testGroup", consumer.ConsumerOption{
    NameServerAddr: "127.0.0.1:9876",
    ConsumerModel:  consumer.Clustering,
    FromWhere:      consumer.ConsumeFromFirstOffset,
})
```

- Subscribe a topic(only support one topic now), and define your consuming function
```go
err := c.Subscribe("test", consumer.MessageSelector{}, func(ctx *consumer.ConsumeMessageContext,
    msgs []*primitive.MessageExt) (consumer.ConsumeResult, error) {
    fmt.Println(msgs)
    return consumer.ConsumeSuccess, nil
})
```
- start the consumer(**NOTE: MUST after subscribe**)

Full examples: [consumer](../examples/consumer)