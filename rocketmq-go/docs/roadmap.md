# RoadMap-Milestone1
## Producer
- [x] ProducerType
    - [x] DefaultProducer
- [ ] API
    - [ ] Send
        - [x] Sync
        - [ ] Async
        - [ ] OneWay
- [ ] Other
    - [x] DelayMessage
    - [x] Config
    - [x] MessageId Generate
    - [x] CompressMsg
    - [x] TimeOut
    - [x] LoadBalance
    - [ ] DefaultTopic
    - [ ] VipChannel
    - [x] Retry
    - [ ] SendMessageHook
    - [ ] CheckRequestQueue
    - [ ] CheckForbiddenHookList
    - [ ] MQFaultStrategy



## Consumer
- [ ] ConsumerType
    - [x] PushConsumer
    - [ ] PullConsumer
- [ ] MessageListener
    - [x] Concurrently
    - [ ] Orderly
- [ ] MessageModel
    - [x] CLUSTERING
    - [ ] BROADCASTING
- [ ] OffsetStore
    - [x] RemoteBrokerOffsetStore
    - [ ] LocalFileOffsetStore
- [x] RebalanceService
- [x] PullMessageService
- [x] ConsumeMessageService
- [ ] AllocateMessageQueueStrategy
    - [x] AllocateMessageQueueAveragely
    - [ ] AllocateMessageQueueAveragelyByCircle
    - [ ] AllocateMessageQueueByConfig
    - [ ] AllocateMessageQueueByMachineRoom
- [ ] Other
    - [x] Config
    - [x] ZIP
    - [ ] AllocateMessageQueueStrategy
    - [x] ConsumeFromWhere
        - [x] CONSUME_FROM_LAST_OFFSET
        - [x] CONSUME_FROM_FIRST_OFFSET
        - [x] CONSUME_FROM_TIMESTAMP
    - [x] Retry(sendMessageBack)
    - [x] TimeOut(clearExpiredMessage)
    - [x] ACK(partSuccess)
    - [x] FlowControl(messageCanNotConsume)
    - [ ] ConsumeMessageHook
    - [ ] filterMessageHookList

## Kernel
- [x] Controller
    - [x] RebalanceController
    - [x] PullMessageController
- [ ] Task
    - [x] Heartbeat
    - [x] UpdateTopicRouteInfoFromNameServer
    - [ ] CleanOfflineBroker
    - [x] PersistAllConsumerOffset
    - [x] ClearExpiredMessage(form consumer consumeMessageService)
    - [ ] UploadFilterClassSource(FromHeartBeat/But Golang Not Easy To do this(Java Source))
- [x] ClientRemotingProcessor
    - [ ] CHECK_TRANSACTION_STATE
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
- [ ] Other
    - [ ] VIPChannel
    - [ ] RPCHook
    
    