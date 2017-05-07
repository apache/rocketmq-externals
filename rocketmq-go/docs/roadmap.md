# RoadMap-Milestone1

## Consumer
- [ ] ConsumerType
    - [ ] PushConsumer
- [ ] MessageListener
    - [ ] Concurrently
- [ ] MessageModel
    - [ ] CLUSTERING
- [ ] OffsetStore
    - [ ] RemoteBrokerOffsetStore
- [ ] RebalanceService
- [ ] PullMessageService
- [ ] ConsumeMessageService
- [ ] AllocateMessageQueueStrategy
    - [ ] AllocateMessageQueueAveragely
- [ ] Other
    - [ ] Config
    - [ ] ZIP
    - [ ] ConsumeFromWhere
        - [ ] CONSUME_FROM_LAST_OFFSET
        - [ ] CONSUME_FROM_FIRST_OFFSET
        - [ ] CONSUME_FROM_TIMESTAMP
    - [ ] Retry(sendMessageBack)
    - [ ] TimeOut(clearExpiredMessage)
    - [ ] ACK(partSuccess)
    - [ ] FlowControl(messageCanNotConsume)
## Manager
- [ ] Controller
    - [ ] PullMessageController
- [ ] Task
    - [ ] Heartbeat
    - [ ] UpdateTopicRouteInfoFromNameServer
    - [ ] PersistAllConsumerOffset
    - [ ] ClearExpiredMessage(form consumer consumeMessageService)
- [ ] ClientRemotingProcessor
    - [ ] CHECK_TRANSACTION_STATE
    - [ ] NOTIFY_CONSUMER_IDS_CHANGED
    - [ ] RESET_CONSUMER_CLIENT_OFFSET
    - [ ] GET_CONSUMER_STATUS_FROM_CLIENT
    - [ ] GET_CONSUMER_RUNNING_INFO
    - [ ] CONSUME_MESSAGE_DIRECTLY

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
    - [ ] PollNameServer
    - [ ] Heartbeat
    - [ ] UpdateTopicRouteInfoFromNameServer
    - [ ] CleanOfflineBroker
    - [ ] PersistAllConsumerOffset
    - [ ] ClearExpiredMessage(form consumer consumeMessageService)
    - [ ] UploadFilterClassSource(FromHeartBeat/But Golang Not Easy To do this(Java Source))
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
    
    