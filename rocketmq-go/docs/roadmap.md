# RoadMap-Milestone1

## Consumer
- [x] ConsumerType
    - [x] PushConsumer
- [x] MessageListener
    - [x] Concurrently
- [x] MessageModel
    - [x] CLUSTERING
- [x] OffsetStore
    - [x] RemoteBrokerOffsetStore
- [x] RebalanceService
- [x] PullMessageService
- [x] ConsumeMessageService
- [x] AllocateMessageQueueStrategy
    - [x] AllocateMessageQueueAveragely
- [x] Other
    - [x] Config
    - [x] ZIP
    - [x] ConsumeFromWhere
        - [x] CONSUME_FROM_LAST_OFFSET
        - [x] CONSUME_FROM_FIRST_OFFSET
        - [x] CONSUME_FROM_TIMESTAMP
    - [x] Retry(sendMessageBack)
    - [x] TimeOut(clearExpiredMessage)
    - [x] ACK(partSuccess)
    - [x] FlowControl(messageCanNotConsume)
    
## Producer
- [x] ProducerType
    - [x] DefaultProducer
- [x] API
    - [x] Send
        - [x] Sync
- [x] Other
    - [x] DelayMessage
    - [x] Config
    - [x] MessageId Generate
    - [x] CompressMsg
    - [x] TimeOut
    - [x] LoadBalance
    - [x] DefaultTopic
    - [x] VipChannel
    - [x] MQFaultStrategy

## Manager
- [x] Controller
    - [x] PullMessageController
- [x] Task
    - [x] UpdateTopicRouteInfo
    - [x] Heartbeat
    - [x] Rebalance
    - [x] PullMessage
    - [x] CleanExpireMsg
- [x] ClientRemotingProcessor
    - [x] CHECK_TRANSACTION_STATE
    - [x] NOTIFY_CONSUMER_IDS_CHANGED
    - [x] RESET_CONSUMER_CLIENT_OFFSET
    - [x] GET_CONSUMER_STATUS_FROM_CLIENT
    - [x] GET_CONSUMER_RUNNING_INFO
    - [x] CONSUME_MESSAGE_DIRECTLY

## Remoting
- [x] MqClientRequest
    - [x] InvokeSync
    - [x] InvokeAsync
    - [x] InvokeOneWay
- [x] Serialize
    - [x] JSON
    - [x] ROCKETMQ
- [x] NamesrvAddrChoosed(HA)


# RoadMap-ALL

## Producer
- [ ] ProducerType
    - [ ] DefaultProducer
    - [ ] TransactionProducer
- [ ] API
    - [ ] Send
        - [ ] Sync
        - [ ] Async
        - [ ] OneWay
- [ ] Other
    - [ ] DelayMessage
    - [ ] Config
    - [ ] MessageId Generate
    - [ ] CompressMsg
    - [ ] TimeOut
    - [ ] LoadBalance
    - [ ] DefaultTopic
    - [ ] VipChannel
    - [ ] Retry
    - [ ] SendMessageHook
    - [ ] CheckRequestQueue
    - [ ] CheckForbiddenHookList
    - [ ] MQFaultStrategy



## Consumer
- [ ] ConsumerType
    - [ ] PushConsumer
    - [ ] PullConsumer
- [ ] MessageListener
    - [ ] Concurrently
    - [ ] Orderly
- [ ] MessageModel
    - [ ] CLUSTERING
    - [ ] BROADCASTING
- [ ] OffsetStore
    - [ ] RemoteBrokerOffsetStore
        - [ ] many actions
    - [ ] LocalFileOffsetStore
- [ ] RebalanceService
- [ ] PullMessageService
- [ ] ConsumeMessageService
- [ ] AllocateMessageQueueStrategy
    - [ ] AllocateMessageQueueAveragely
    - [ ] AllocateMessageQueueAveragelyByCircle
    - [ ] AllocateMessageQueueByConfig
    - [ ] AllocateMessageQueueByMachineRoom
- [ ] Other
    - [ ] Config
    - [ ] ZIP
    - [ ] AllocateMessageQueueStrategy
    - [ ] ConsumeFromWhere
        - [ ] CONSUME_FROM_LAST_OFFSET
        - [ ] CONSUME_FROM_FIRST_OFFSET
        - [ ] CONSUME_FROM_TIMESTAMP
    - [ ] Retry(sendMessageBack)
    - [ ] TimeOut(clearExpiredMessage)
    - [ ] ACK(partSuccess)
    - [ ] FlowControl(messageCanNotConsume)
    - [ ] ConsumeMessageHook
    - [ ] filterMessageHookList

## Manager
- [ ] Controller
    - [ ] RebalanceController
    - [ ] PullMessageController
- [ ] Task
    - [ ] UpdateTopicRouteInfo
    - [ ] Heartbeat
    - [ ] Rebalance
    - [ ] PullMessage
    - [ ] CleanExpireMsg
- [ ] ClientRemotingProcessor
    - [ ] CHECK_TRANSACTION_STATE
    - [ ] NOTIFY_CONSUMER_IDS_CHANGED
    - [ ] RESET_CONSUMER_CLIENT_OFFSET
    - [ ] GET_CONSUMER_STATUS_FROM_CLIENT
    - [ ] GET_CONSUMER_RUNNING_INFO
    - [ ] CONSUME_MESSAGE_DIRECTLY
## Remoting
- [ ] MqClientRequest
    - [ ] InvokeSync
    - [ ] InvokeAsync
    - [ ] InvokeOneWay
- [ ] Serialize
    - [ ] JSON
    - [ ] ROCKETMQ
- [ ] NamesrvAddrChoosed(HA)
- [ ] Other
    - [ ] VIPChannel
    - [ ] RPCHook
    
    