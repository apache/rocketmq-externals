package rocketmq

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util/structs"
	"github.com/golang/glog"
	"strings"
	"sync"
	"time"
)

//@see com.alibaba.rocketmq.client.impl.factory.MQClientInstance
//所有的consumer producer都交有这个类来管理
//目前似乎不能搞多个namesvr的

//这个是一个name——svr下的
type MqClientManager struct {
	rocketMqManagerLock sync.Mutex
	//ClientId            string
	BootTimestamp int64

	clientFactory *ClientFactory

	NamesrvLock   sync.Mutex
	HeartBeatLock sync.Mutex
	//all producer and consumer use this
	mqClient service.RocketMqClient //暂时是一个临时变量的作用
	//all producer and consumer use this
	//private final ClientRemotingProcessor clientRemotingProcessor;
	//	private final PullMessageService pullMessageService;
	//private final RebalanceService rebalanceService;
	//	private final ConsumerStatsManager consumerStatsManager;
	//	private final AtomicLong storeTimesTotal = new AtomicLong(0);
	ServiceState int //0 初始状态 1运行中 2关闭

	//should be here because need all producer consumer
	pullMessageController    *PullMessageController
	cleanExpireMsgController *CleanExpireMsgController
	rebalanceControllr       *RebalanceController
	//should be here because need all producer consumer
	defaultProducerService *service.DefaultProducerService //for send back message
}

func MqClientManagerInit(clientConfig *config.ClientConfig) (rocketMqManager *MqClientManager) {
	rocketMqManager = &MqClientManager{}
	rocketMqManager.BootTimestamp = time.Now().Unix()
	rocketMqManager.clientFactory = ClientFactoryInit()
	rocketMqManager.mqClient = service.MqClientInit(clientConfig, rocketMqManager.InitClientRequestProcessor()) // todo todo todo
	rocketMqManager.pullMessageController = NewPullMessageController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	rocketMqManager.cleanExpireMsgController = NewCleanExpireMsgController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	rocketMqManager.rebalanceControllr = NewRebalanceController(rocketMqManager.clientFactory)

	return
}

//CHECK_TRANSACTION_STATE
//NOTIFY_CONSUMER_IDS_CHANGED
//RESET_CONSUMER_CLIENT_OFFSET
//GET_CONSUMER_STATUS_FROM_CLIENT
//GET_CONSUMER_RUNNING_INFO
//CONSUME_MESSAGE_DIRECTLY
func (self *MqClientManager) InitClientRequestProcessor() (clientRequestProcessor remoting.ClientRequestProcessor) {
	clientRequestProcessor = func(cmd *remoting.RemotingCommand) (response *remoting.RemotingCommand) {
		switch cmd.Code {
		case remoting.CHECK_TRANSACTION_STATE:
			glog.V(2).Info("receive_request_code CHECK_TRANSACTION_STATE")
			// todo this version don't impl this
			break
		case remoting.NOTIFY_CONSUMER_IDS_CHANGED:
			glog.V(1).Info("receive_request_code NOTIFY_CONSUMER_IDS_CHANGED")
			self.rebalanceControllr.doRebalance()
			break
		case remoting.RESET_CONSUMER_CLIENT_OFFSET: //  struct json key supported
			glog.V(2).Info("receive_request_code RESET_CONSUMER_CLIENT_OFFSET")
			glog.V(2).Info("op=look cmd body", string(cmd.Body))
			var resetOffsetRequestHeader = &header.ResetOffsetRequestHeader{}
			if cmd.ExtFields != nil {
				resetOffsetRequestHeader.FromMap(cmd.ExtFields) //change map[string]interface{} into CustomerHeader struct
				glog.V(2).Info("op=look ResetOffsetRequestHeader", resetOffsetRequestHeader)
				resetOffsetBody := &model.ResetOffsetBody{}
				err := resetOffsetBody.Decode(cmd.Body)
				if err != nil {
					return
				}
				glog.V(2).Info("op=look resetOffsetBody xxxxx", resetOffsetBody)
				self.resetConsumerOffset(resetOffsetRequestHeader.Topic, resetOffsetRequestHeader.Group, resetOffsetBody.OffsetTable)
			}
			break
		case remoting.GET_CONSUMER_STATUS_FROM_CLIENT: // useless we can use GET_CONSUMER_RUNNING_INFO instead
			glog.V(2).Info("receive_request_code GET_CONSUMER_STATUS_FROM_CLIENT")
			break
		case remoting.GET_CONSUMER_RUNNING_INFO:
			glog.V(2).Info("receive_request_code GET_CONSUMER_RUNNING_INFO")
			var getConsumerRunningInfoRequestHeader = &header.GetConsumerRunningInfoRequestHeader{}
			if cmd.ExtFields != nil {
				getConsumerRunningInfoRequestHeader.FromMap(cmd.ExtFields) //change map[string]interface{} into CustomerHeader struct
				consumerRunningInfo := model.ConsumerRunningInfo{}
				consumerRunningInfo.Properties = map[string]string{}
				defaultMQPushConsumer := self.clientFactory.ConsumerTable[getConsumerRunningInfoRequestHeader.ConsumerGroup]
				consumerConfigMap := structs.Map(defaultMQPushConsumer.ConsumerConfig) // todo test
				for key, value := range consumerConfigMap {
					consumerRunningInfo.Properties[key] = fmt.Sprintf("%v", value)
				}

				consumerRunningInfo.Properties["PROP_NAMESERVER_ADDR"] = strings.Join(defaultMQPushConsumer.mqClient.GetRemotingClient().GetNamesrvAddrList(), ";")
				consumerRunningInfo.MqTable = defaultMQPushConsumer.rebalance.GetMqTableInfo()

				glog.V(2).Info("op=look consumerRunningInfo", consumerRunningInfo)
				jsonByte, err := consumerRunningInfo.Encode()
				glog.V(2).Info("op=enCode jsonByte", string(jsonByte))
				if err != nil {
					glog.Error(err)
					return
				}
				response = remoting.NewRemotingCommandWithBody(remoting.SUCCESS, nil, jsonByte)
			}

			break
		case remoting.CONSUME_MESSAGE_DIRECTLY:
			glog.V(2).Info("receive_request_code CONSUME_MESSAGE_DIRECTLY")
			var consumeMessageDirectlyResultRequestHeader = &header.ConsumeMessageDirectlyResultRequestHeader{}
			if cmd.ExtFields != nil {
				consumeMessageDirectlyResultRequestHeader.FromMap(cmd.ExtFields)
				messageExt := &DecodeMessage(cmd.Body)[0]
				glog.V(2).Info("op=look", messageExt)
				defaultMQPushConsumer := self.clientFactory.ConsumerTable[consumeMessageDirectlyResultRequestHeader.ConsumerGroup]
				consumeResult, err := defaultMQPushConsumer.consumeMessageService.ConsumeMessageDirectly(messageExt, consumeMessageDirectlyResultRequestHeader.BrokerName)
				if err != nil {
					return
				}
				jsonByte, err := json.Marshal(consumeResult)
				if err != nil {
					glog.Error(err)
					return
				}
				response = remoting.NewRemotingCommandWithBody(remoting.SUCCESS, nil, jsonByte)
			}
		default:
			glog.Error("illeage requestCode ", cmd.Code)
		}
		return
	}
	return
}
func (self *MqClientManager) RegistProducer(producer *DefaultMQProducer) {
	producer.producerService = service.NewDefaultProducerService(producer.producerGroup, producer.ProducerConfig, self.mqClient)
	self.clientFactory.ProducerTable[producer.producerGroup] = producer
	return
}

func (self *MqClientManager) resetConsumerOffset(topic, group string, offsetTable map[model.MessageQueue]int64) {
	consumer := self.clientFactory.ConsumerTable[group]
	if consumer == nil {
		glog.Error("resetConsumerOffset beacuse consumer not online,group=", group)
		return
	}
	consumer.resetOffset(offsetTable)
}
func (self *MqClientManager) RegistConsumer(consumer *DefaultMQPushConsumer) {
	if self.defaultProducerService == nil {
		self.defaultProducerService = service.NewDefaultProducerService(constant.CLIENT_INNER_PRODUCER_GROUP, config.NewProducerConfig(), self.mqClient)
	}
	consumer.mqClient = self.mqClient
	consumer.offsetStore = service.RemoteOffsetStoreInit(consumer.consumerGroup, self.mqClient)
	self.clientFactory.ConsumerTable[consumer.consumerGroup] = consumer
	consumer.rebalance = service.NewRebalance(consumer.consumerGroup, consumer.subscription, consumer.mqClient, consumer.offsetStore, consumer.ConsumerConfig)

	fmt.Println(consumer.consumeMessageService)

	consumer.consumeMessageService.Init(consumer.consumerGroup, self.mqClient, consumer.offsetStore, self.defaultProducerService, consumer.ConsumerConfig)
	return
}

func (self *MqClientManager) Start() {
	//self.SendHeartbeatToAllBrokerWithLock()//we should send heartbeat first
	self.StartAllScheduledTask()
}

func (self MqClientManager) ShutDown() {

}

type ClientFactory struct {
	ProducerTable map[string]*DefaultMQProducer     //group|RocketMQProducer
	ConsumerTable map[string]*DefaultMQPushConsumer //group|Consumer
}

func ClientFactoryInit() (clientFactory *ClientFactory) {
	clientFactory = &ClientFactory{}
	clientFactory.ProducerTable = make(map[string]*DefaultMQProducer)
	clientFactory.ConsumerTable = make(map[string]*DefaultMQPushConsumer)
	return
}

//heart beat
func (self MqClientManager) SendHeartbeatToAllBrokerWithLock() error {
	heartbeatData := self.prepareHeartbeatData()
	if len(heartbeatData.ConsumerDataSet) == 0 {
		return errors.New("send heartbeat error")
	}
	self.mqClient.SendHeartbeatToAllBroker(heartbeatData)
	return nil
}

//routeInfo
func (self MqClientManager) UpdateTopicRouteInfoFromNameServer() {
	var topicSet []string
	for _, consumer := range self.clientFactory.ConsumerTable {
		for key, _ := range consumer.subscription {
			topicSet = append(topicSet, key)
		}
	}
	topicSet = append(topicSet, self.mqClient.GetPublishTopicList()...)
	for _, topic := range topicSet {
		self.mqClient.UpdateTopicRouteInfoFromNameServer(topic)

	}
}

func (self MqClientManager) prepareHeartbeatData() *model.HeartbeatData {
	heartbeatData := new(model.HeartbeatData)
	heartbeatData.ClientId = self.mqClient.GetClientId()
	heartbeatData.ConsumerDataSet = make([]*model.ConsumerData, 0)
	heartbeatData.ProducerDataSet = make([]*model.ProducerData, 0)
	for group, consumer := range self.clientFactory.ConsumerTable {
		consumerData := new(model.ConsumerData)
		consumerData.GroupName = group
		consumerData.ConsumeType = consumer.consumeType
		consumerData.ConsumeFromWhere = consumer.ConsumerConfig.ConsumeFromWhere
		consumerData.MessageModel = consumer.messageModel
		consumerData.SubscriptionDataSet = consumer.Subscriptions()
		consumerData.UnitMode = consumer.unitMode
		heartbeatData.ConsumerDataSet = append(heartbeatData.ConsumerDataSet, consumerData)
	}
	for group := range self.clientFactory.ProducerTable {
		producerData := new(model.ProducerData)
		producerData.GroupName = group
		heartbeatData.ProducerDataSet = append(heartbeatData.ProducerDataSet, producerData)
	}
	return heartbeatData
}
