package producer

import (
	topic "../"
	"../client"
	"../message"
	ptc "../protocol"
	"errors"
	"fmt"
	"sync"
	"time"
)

type SendCallback interface {
	OnSuccess(status ptc.SendResult)
	OnError(err error)
}

type MessageQueueSelector interface { //TODO use func
	Select(mqs []*msg.MessageQueue, msg msg.Message, arg interface{}) msg.MessageQueue
}

var selectorByHash = func(mqs []*msg.MessageQueue, msg msg.Message, arg interface{}) msg.MessageQueue {
	// TODO
	return nil
}

type MQProducer interface {
	ptc.MQAdmin
	Start() error
	Shutdown()
	FetchPublishMessageQueues(topic string) ([]*msg.MessageQueue, error)
	Send(msg msg.Message, mq msg.MessageQueue, selector MessageQueueSelector, callback SendCallback, timeout time.Duration) (ptc.SendResult, error)
	SendOneWayWithSelector(msg msg.Message, mq msg.MessageQueue, selector MessageQueueSelector, arg interface{}) (ptc.SendResult, error)
	//SendMessageInTransaction(msg msg.Message, tranExecuter LocalTransactionExecuter, arg interface{}g) (Transactionptc.SendResult error)
}

type MQProducerInner interface {
	PublishTopicList() map[string]string
	IsPublishTopicNeedUpdate(topic string) bool
	UpdatePublishTopicList(topic string, info ptc.TopicPublishInfo) bool
	IsUnitMode()
	// TODO about transaction
}

type ServiceState int

const (
	CreateJust ServiceState = iota
	Running
	ShutdownAlready
	StartFailed
)

type DefaultProducerImpl struct {
}

type RPCHook struct {
}

type DefaultProducer struct {
	client.ClientConfig
	mutex          sync.Mutex
	state          ServiceState
	rpcHook        RPCHook
	mqclient       *ptc.MQClientInstance
	topicInfoTable map[string]*ptc.TopicPublishInfo

	producerGroup                    string
	createTopicKey                   string
	defaultTopicQueueNum             int //TODO default value
	sendMsgTimeout                   time.Duration
	compressMsgBodyThreshold         int
	retryTimesWhenSendFailed         int //Todo async and sync?
	retryAnotherBrokerWhenNotStoreOK bool
	maxMessageSize                   int
}

func NewDefaultProducer(groupName string) *DefaultProducer {
	return &DefaultProducer{
		producerGroup:                    groupName,
		createTopicKey:                   "defaultTopic",
		defaultTopicQueueNum:             4,
		sendMsgTimeout:                   3000,
		compressMsgBodyThreshold:         1 << 12,
		retryTimesWhenSendFailed:         2,
		retryAnotherBrokerWhenNotStoreOK: false,
		maxMessageSize:                   1 << 22,
	}
}

func (producer *DefaultProducer) SetNameServerAddress(s string) {
	producer.SetNameServerAddress(s)
}

func (producer *DefaultProducer) Start() error {
	producer.mutex.Lock()
	defer producer.mutex.Unlock()

	switch producer.state {
	case CreateJust:
		producer.state = StartFailed
		if err := producer.checkConfig(); err != nil {
			return err
		}

		// TODO
		//if (!this.defaultMQProducer.getProducerGroup().equals(MixAll.CLIENT_INNER_PRODUCER_GROUP)) {
		//	this.defaultMQProducer.changeInstanceNameToPID();
		//}
		producer.mqclient = client.CreateMQClientInstance(&producer.ClientConfig, nil)
		registerOK := producer.mqclient.RegisterProducer(producer.producerGroup, producer)
		if !registerOK {
			producer.state = CreateJust
			return errors.New(fmt.Sprintf("The producer group[%s] has been created before, specify another name please.",
				producer.producerGroup))
		}
		producer.topicInfoTable[producer.createTopicKey] = &ptc.TopicPublishInfo{}
		producer.mqclient.Start()
		fmt.Printf("the producer [%s] start OK. sendMessageWithVIPChannel=%s", producer.producerGroup, producer.isSendMessageWithVIPChannel())
		producer.state = Running
	case Running, StartFailed, ShutdownAlready:
		return errors.New(fmt.Sprint("The producer service state not OK, maybe started once, ", producer.state))
	}
	return nil
}

func (producer *DefaultProducer) isSendMessageWithVIPChannel() bool {
	return false
}

func (producer *DefaultProducer) checkConfig() error {
	//CheckGroup(producer.producerGroup)

	if producer.producerGroup == "" {
		return errors.New("producerGroup is null")
	}

	if producer.producerGroup == "DEFAULT" {
		return errors.New(fmt.Sprintf("producerGroup can not equal %s, please specify another one.", "MixAll.DEFAULT_PRODUCER_GROUP"))
	}
	return nil
}

func (producer *DefaultProducer) Send(msg *msg.Message) (result *ptc.SendResult, err error) {
	return producer.sendDefault(msg)
}

func (producer *DefaultProducer) sendDefault(message *msg.Message /*TODO*/) (result *ptc.SendResult, err error) {
	producer.makeSureStateOK()
	// Validators.checkMessage(msg, this.defaultMQProducer);
	//invokeID := rand.Int63()
	beginTimestampFirst := time.Now().Nanosecond()
	beginTimestampPrev := beginTimestampFirst
	endTimestamp := beginTimestampFirst
	topicPublishInfo := producer.tryToFindTopicPublishInfo(message.Topic)
	if topicPublishInfo != nil && topicPublishInfo.Ok() {
		var mq *msg.MessageQueue
		timesTotal, times := 1, 0
		brokerSent := make([]string, timesTotal)
		for ; times < timesTotal; times++ {
			var lastBrokerName string // TODO
			tmpmq := producer.selectOneMessageQueue(topicPublishInfo, lastBrokerName)
			if tmpmq != nil {
				mq = tmpmq
				brokerSent[times] = mq.BrokerName()
				beginTimestampPrev = time.Now().Nanosecond()
				result = producer.sendKernel(msg, mq /*commu*/, topicPublishInfo)
				endTimestamp = time.Now().Nanosecond()
				producer.updateFaultItem(mq.BrokerName(), endTimestamp-beginTimestampPrev, false)
			}
		}
	}
	return
}

func (producer *DefaultProducer) updateFaultItem(brokerName string, currentLatency int, isolation bool) {

}

type SendMessageContext struct {
}

type SendMessageRequestHeader struct {
	producerGroup        string
	topic                string
	dafaultTopic         string
	defaultTopicQueueNum int
	queueID              int
	sysFlag              int
	bornTimestamp        int
	flag                 int
	properties           string
	reconsumeTimes       int
	unitMode             bool
	maxReconsumeTimes    int
}

func (producer *DefaultProducer) sendKernel(msg *msg.Message, mq *msg.MessageQueue, info *ptc.TopicPublishInfo) (result *ptc.SendResult) {
	brokerAddress := producer.mqclient.FindBrokerAddressInPublish(mq.BrokerName())
	var context SendMessageContext
	if brokerAddress != "" {
		//prevBody := msg.Body()

		//MessageClientIDSetter.setUniqID(msg);
		sysFlag := 0
		if producer.tryToCompressMessage(msg) {
			sysFlag |= 1
		}
		//TODO
		requestHeader := &SendMessageRequestHeader{
			producerGroup:        producer.producerGroup,
			topic:                msg.Topic,
			dafaultTopic:         producer.createTopicKey,
			defaultTopicQueueNum: producer.defaultTopicQueueNum,
			queueID:              mq.QueudID,
			sysFlag:              sysFlag,
			bornTimestamp:        time.Now().Nanosecond(),
			reconsumeTimes:       0,
			unitMode:             producer.UnitMode(),
		}
		result = producer.mqclient.MQClientAPI().SendMessage(brokerAddress, mq.BrokerName(),
			msg, requestHeader, 100, sysFlag, 1, context, producer)

	}
	return nil
}

func (producer *DefaultProducer) tryToCompressMessage(msg *msg.Message) bool {
	return false
}

func (producer *DefaultProducer) selectOneMessageQueue(info *ptc.TopicPublishInfo, lastBrokerName string) *msg.MessageQueue {
	return nil
}

func (producer *DefaultProducer) tryToFindTopicPublishInfo(topic string) *ptc.TopicPublishInfo {
	return &ptc.TopicPublishInfo{}
}

func (producer *DefaultProducer) makeSureStateOK() {

}
