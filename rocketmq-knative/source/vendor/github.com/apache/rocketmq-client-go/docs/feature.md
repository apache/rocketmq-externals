# Feature

## Producer

### MessageType
- [x] NormalMessage
- [ ] TransactionMessage
- [ ] DelayMessage

### SendWith    
- [x] Sync
- [ ] Async
- [x] OneWay

### Other    
- [ ] Config
- [ ] MessageId Generate
- [ ] CompressMsg
- [ ] LoadBalance
- [ ] DefaultTopic
- [ ] VipChannel
- [ ] Retry
- [ ] Hook
- [ ] CheckRequestQueue
- [ ] MQFaultStrategy

## Consumer

### ReceiveType
- [x] Push
- [ ] Pull

### ConsumingType
- [x] Concurrently
- [ ] Orderly

### MessageModel
- [x] CLUSTERING
- [x] BROADCASTING
    
### AllocateMessageQueueStrategy
- [x] AllocateMessageQueueAveragely
- [x] AllocateMessageQueueAveragelyByCircle
- [X] AllocateMessageQueueByConfig
- [X] AllocateMessageQueueByMachineRoom

### Other
- [x] Rebalance
- [x] Flow Control
- [ ] compress
- [x] ConsumeFromWhere
- [ ] Retry(sendMessageBack)
- [ ] Hook

## Common
- [ ] PollNameServer
- [x] Heartbeat
- [x] UpdateTopicRouteInfoFromNameServer
- [ ] CleanOfflineBroker
- [ ] ClearExpiredMessage(form consumer consumeMessageService)
    
## Remoting
- [x] API
    - [x] InvokeSync
    - [x] InvokeAsync
    - [x] InvokeOneWay
- [x] Serialize
    - [x] JSON
    - [x] ROCKETMQ
- [ ] Other
    - [ ] VIPChannel
    - [ ] RPCHook
    