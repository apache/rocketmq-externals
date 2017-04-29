package consumer

import (
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"errors"
	"fmt"
)

const (
	PullTimeDelayMillsWhenError time.Duration = 3 * time.Second
	// Flow control interval
	PullTimeDelayMillsWhenFlowControl time.Duration = 50 * time.Millisecond
	// Delay some time when suspend pull service
	PullTimeDelayMillsWhenSuspend time.Duration = 1 * time.Second
	BrokerSuspendMaxTimeMills time.Duration = 15 * time.Second
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
	ConsumeActively ConsumeType = "PULL"
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
	rebalance service.Rebalance
	//filterMessageHookList
	consumerStartTimestamp time.Time
	//consumeMessageHookList
	rpcHook remoting.RPCHook
	status ServiceStatus
	//	mQClientFactory
	//pullAPIWrapper
	pause bool
	consumeOrderly bool
	//messageListenerInner
	offsetStore service.OffsetStore
	consumeMessageService service.ConsumeMessageService
	flowControlTimes1 int64
	flowControlTimes2 int64

	api *service.MQClientAPI
}

func NewMQPushConsumer() MQPushConsumer {
	return nil
}

func (dpc *MQPushConsumer) SendMessageBack(msgX *message.MessageExt, delayLevel int, brokerName string) error {
	return nil
}

func (dpc *MQPushConsumer) FetchSubscribeMessageQueues(topic string) ([]*message.MessageQueue, error) {
	result := dpc.rebalance.TopicSubscribeInfoTable()[topic]

	if result == nil {
		// TODO updateTopicRouteInfoFromNameServer
		result = dpc.rebalance.TopicSubscribeInfoTable()[topic]
	}

	if result == nil {
		return nil, errors.New(fmt.Sprintf("The topic %s not exist", topic))
	}
	return result, nil
}

func (dpc *MQPushConsumer) makeSureStatusOK() error {
	return nil
}

