package consumer

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"time"
)

const (
	PullTimeDelayMillsWhenError time.Duration = 3 * time.Second
	// Flow control interval
	PullTimeDelayMillsWhenFlowControl time.Duration = 50 * time.Millisecond
	// Delay some time when suspend pull service
	PullTimeDelayMillsWhenSuspend   time.Duration = 1 * time.Second
	BrokerSuspendMaxTimeMills       time.Duration = 15 * time.Second
	ConsumerTimeoutMillsWhenSuspend time.Duration = 30 * time.Second
)

type ServiceStatus int

const (
	CreateJust ServiceStatus = iota
	Running
	ShutdownAlready
	StartFailed
)

type MessageModel int

const (
	Broadcasting MessageModel = iota
	Clustering
)

type ConsumeFromWhere int

const (
	ConsumeFromLastOffset ConsumeFromWhere = iota
	ConsumeFromFirstOffset
	ConsumeFromTimestamp
)

type ConsumeType string

const (
	ConsumeActively  ConsumeType = "PULL"
	ConsumePassively ConsumeType = "PUSH"
)

type MQConsumer interface {
	SendMessageBack(msgX *message.MessageExt, delayLevel int, brokerName string) error
	FetchSubscribeMessageQueues(topic string) ([]*message.MessageQueue, error)

	groupName() string
	messageModel() MessageModel
	consumeType() ConsumeType
	consumeFromWhere() ConsumeFromWhere
	doRebalance()
	persistConsumerOffset()
	updateTopicSubscribeInfo(topic string, info []*message.MessageQueue)
	unitMode() bool
	runningInfo() runningInfo
}

type MQPushConsumer struct {
	//filterMessageHookList
	consumerStartTimestamp time.Time
	//consumeMessageHookList
	rpcHook remoting.RPCHook
	status  ServiceStatus
	//	mQClientFactory
	//pullAPIWrapper
	pause          bool
	consumeOrderly bool
	//messageListenerInner
	flowControlTimes1 int64
	flowControlTimes2 int64

	api *service.MQClientAPI
}

type DefaultMQPushConsumer struct { // 直接按照impl写
	mqClient              service.RocketMqClient
	consumeMessageService service.ConsumeMessageService
	//ConsumerConfig        *MqConsumerConfig
	clientConfig     *config.ClientConfig
	consumerGroup    string
	messageModel     MessageModel
	consumeFromWhere ConsumeFromWhere
	consumeTimestamp time.Time
	// AllocateMessageQueueStrategy
	rebalance     *service.Rebalance //Rebalance's impl depend on offsetStore
	subscriptions map[string]string
	// TODO MessageListener
	offsetStore service.OffsetStore //for consumer's offset

	consumeConcurrentMin       int
	consumeConcurrentMax       int
	adjustChannelSizeThreshold int
	consumeConcurrentlyMaxSpan int
	pullThresholdForQueue      int
	pullInterval               time.Duration
	consumeMessageBatchMaxSize int
	pullBatchSize              int
	postSubscriptionWhenPull   bool
	unitMode                   bool
	maxReconsumeTimes          int
	// TODO queue -> chan?
	suspendCurrentQueueTimeMillis time.Duration
	consumeTimeout                time.Duration

	quit chan int
}

func NewMQPushConsumer() MQPushConsumer {
	return nil
}

func NewDefaultPushConsumer() *DefaultMQPushConsumer {
	return &DefaultMQPushConsumer{
		mqClient:              nil,
		consumeMessageService: nil,
		clientConfig:          nil,
		consumerGroup:         "default",
		messageModel:          Clustering,
		consumeFromWhere:      ConsumeFromLastOffset,
		consumeTimestamp:      time.Now(), // TODO get from env
		// AllocateMessageQueueStrategy
		rebalance:     nil,
		subscriptions: make(map[string]string),
		// TODO MessageListener
		offsetStore: nil,

		consumeConcurrentMin:       20,
		consumeConcurrentMax:       64,
		adjustChannelSizeThreshold: 1000,
		consumeConcurrentlyMaxSpan: 2000,
		pullThresholdForQueue:      1000,
		pullInterval:               0,
		consumeMessageBatchMaxSize: 1,
		pullBatchSize:              32,
		postSubscriptionWhenPull:   false,
		unitMode:                   false,
		maxReconsumeTimes:          -1,
		// TODO queue -> chan?
		suspendCurrentQueueTimeMillis: 1 * time.Second,
		consumeTimeout:                15 * time.Millisecond,

		quit: make(chan int),
	}
}

func (dpc *MQPushConsumer) SendMessageBack(msgX *message.MessageExt, delayLevel int, brokerName string) error {
	return nil
}

func (dpc *MQPushConsumer) FetchSubscribeMessageQueues(topic string) ([]*message.MessageQueue, error) {
	//result := dpc.rebalance.TopicSubscribeInfoTable()[topic]

	//if result == nil {
	//	// TODO updateTopicRouteInfoFromNameServer
	//	result = dpc.rebalance.TopicSubscribeInfoTable()[topic]
	//}
	//
	//if result == nil {
	//	return nil, errors.New(fmt.Sprintf("The topic %s not exist", topic))
	//}
	return nil, nil
}

func (dpc *MQPushConsumer) makeSureStatusOK() error {
	return nil
}
