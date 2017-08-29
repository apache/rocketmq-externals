/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kernel

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"strings"
	"time"
)

//MqClientManager MqClientManager
type MqClientManager struct {
	//rocketMqManagerLock      sync.Mutex
	BootTimestamp            int64
	clientFactory            *clientFactory
	mqClient                 RocketMqClient
	pullMessageController    *PullMessageController
	cleanExpireMsgController *cleanExpireMsgController
	rebalanceControllr       *rebalanceController
	defaultProducerService   *DefaultProducerService
}

//MqClientManagerInit create a MqClientManager instance
func MqClientManagerInit(clientConfig *rocketmqm.MqClientConfig) (rocketMqManager *MqClientManager) {
	rocketMqManager = &MqClientManager{}
	rocketMqManager.BootTimestamp = time.Now().Unix()
	rocketMqManager.clientFactory = clientFactoryInit()
	rocketMqManager.mqClient = mqClientInit(clientConfig, rocketMqManager.initClientRequestProcessor()) // todo todo todo
	rocketMqManager.pullMessageController = newPullMessageController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	rocketMqManager.cleanExpireMsgController = newCleanExpireMsgController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	rocketMqManager.rebalanceControllr = newRebalanceController(rocketMqManager.clientFactory)

	return
}

//Start start MqClientManager
func (m *MqClientManager) Start() {
	//d.sendHeartbeatToAllBrokerWithLock()//we should send heartbeat first todo check
	m.startAllScheduledTask()
}

//RegisterProducer register producer to this MqClientManager
func (m *MqClientManager) RegisterProducer(producer *DefaultMQProducer) {
	producer.producerService = newDefaultProducerService(producer.producerGroup, producer.ProducerConfig, m.mqClient)
	m.clientFactory.producerTable[producer.producerGroup] = producer
	return
}

//RegisterConsumer register consumer to this MqClientManager
func (m *MqClientManager) RegisterConsumer(consumer *DefaultMQPushConsumer) {
	if m.defaultProducerService == nil {
		m.defaultProducerService = newDefaultProducerService(constant.CLIENT_INNER_PRODUCER_GROUP, rocketmqm.NewProducerConfig(), m.mqClient)
	}
	consumer.mqClient = m.mqClient
	consumer.offsetStore = remoteOffsetStoreInit(consumer.consumerGroup, m.mqClient)
	m.clientFactory.consumerTable[consumer.consumerGroup] = consumer
	consumer.rebalance = newRebalance(consumer.consumerGroup, consumer.subscription, consumer.mqClient, consumer.offsetStore, consumer.ConsumerConfig)
	consumer.consumeMessageService.init(consumer.consumerGroup, m.mqClient, consumer.offsetStore, m.defaultProducerService, consumer.ConsumerConfig)
	return
}
func (m *MqClientManager) initClientRequestProcessor() (clientRequestProcessor remoting.ClientRequestProcessor) {
	clientRequestProcessor = func(cmd *remoting.RemotingCommand) (response *remoting.RemotingCommand) {
		switch cmd.Code {
		case remoting.CHECK_TRANSACTION_STATE:
			glog.V(2).Info("receive_request_code CHECK_TRANSACTION_STATE")
			break
		case remoting.NOTIFY_CONSUMER_IDS_CHANGED:
			glog.V(2).Info("receive_request_code NOTIFY_CONSUMER_IDS_CHANGED")
			m.rebalanceControllr.doRebalance()
			break
		case remoting.RESET_CONSUMER_CLIENT_OFFSET: //  struct json key supported
			m.resetConsumerClientOffset(cmd)
			break
		case remoting.GET_CONSUMER_STATUS_FROM_CLIENT: // useless we can use GET_CONSUMER_RUNNING_INFO instead
			glog.V(2).Info("receive_request_code GET_CONSUMER_STATUS_FROM_CLIENT")
			break
		case remoting.GET_CONSUMER_RUNNING_INFO:
			response = m.getConsumerRunningInfo(cmd)
			break
		case remoting.CONSUME_MESSAGE_DIRECTLY:
			response = m.consumeMessageDirectly(cmd)
			break
		default:
			glog.Error("illeage requestCode ", cmd.Code)
		}
		return
	}
	return
}

func (m *MqClientManager) consumeMessageDirectly(cmd *remoting.RemotingCommand) (response *remoting.RemotingCommand) {
	glog.V(2).Info("receive_request_code CONSUME_MESSAGE_DIRECTLY")
	var consumeMessageDirectlyResultRequestHeader = &header.ConsumeMessageDirectlyResultRequestHeader{}
	if cmd.ExtFields != nil {
		consumeMessageDirectlyResultRequestHeader.FromMap(cmd.ExtFields)
		messageExt := &decodeMessage(cmd.Body)[0]
		glog.V(2).Info("op=look", messageExt)
		defaultMQPushConsumer := m.clientFactory.consumerTable[consumeMessageDirectlyResultRequestHeader.ConsumerGroup]
		consumeResult, err := defaultMQPushConsumer.consumeMessageService.consumeMessageDirectly(messageExt, consumeMessageDirectlyResultRequestHeader.BrokerName)
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
	return
}

func (m *MqClientManager) getConsumerRunningInfo(cmd *remoting.RemotingCommand) (response *remoting.RemotingCommand) {
	glog.V(2).Info("receive_request_code GET_CONSUMER_RUNNING_INFO")
	var getConsumerRunningInfoRequestHeader = &header.GetConsumerRunningInfoRequestHeader{}
	if cmd.ExtFields != nil {
		getConsumerRunningInfoRequestHeader.FromMap(cmd.ExtFields) //change map[string]interface{} into CustomerHeader struct
		consumerRunningInfo := model.ConsumerRunningInfo{}
		consumerRunningInfo.Properties = map[string]string{}
		defaultMQPushConsumer := m.clientFactory.consumerTable[getConsumerRunningInfoRequestHeader.ConsumerGroup]
		consumerConfigMap := util.Struct2Map(defaultMQPushConsumer.ConsumerConfig)
		for key, value := range consumerConfigMap {
			consumerRunningInfo.Properties[key] = fmt.Sprintf("%v", value)
		}

		consumerRunningInfo.Properties["PROP_NAMESERVER_ADDR"] = strings.Join(defaultMQPushConsumer.mqClient.getRemotingClient().GetNamesrvAddrList(), ";")
		consumerRunningInfo.MqTable = defaultMQPushConsumer.rebalance.getMqTableInfo()

		glog.V(2).Info("op=look consumerRunningInfo", consumerRunningInfo)
		jsonByte, err := consumerRunningInfo.Encode()
		glog.V(2).Info("op=enCode jsonByte", string(jsonByte))
		if err != nil {
			glog.Error(err)
			return
		}
		response = remoting.NewRemotingCommandWithBody(remoting.SUCCESS, nil, jsonByte)
	}
	return
}
func (m *MqClientManager) resetConsumerClientOffset(cmd *remoting.RemotingCommand) {
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
		m.resetConsumerOffset(resetOffsetRequestHeader.Topic, resetOffsetRequestHeader.Group, resetOffsetBody.OffsetTable)
	}
}

func (m *MqClientManager) resetConsumerOffset(topic, group string, offsetTable map[rocketmqm.MessageQueue]int64) {
	consumer := m.clientFactory.consumerTable[group]
	if consumer == nil {
		glog.Error("resetConsumerOffset because consumer not online,group=", group)
		return
	}
	consumer.resetOffset(offsetTable)
}

type clientFactory struct {
	producerTable map[string]*DefaultMQProducer     //group|RocketMQProducer
	consumerTable map[string]*DefaultMQPushConsumer //group|Consumer
}

func clientFactoryInit() (clientFactoryInstance *clientFactory) {
	clientFactoryInstance = &clientFactory{}
	clientFactoryInstance.producerTable = make(map[string]*DefaultMQProducer)
	clientFactoryInstance.consumerTable = make(map[string]*DefaultMQPushConsumer)
	return
}

//heart beat
func (m *MqClientManager) sendHeartbeatToAllBrokerWithLock() error {
	heartbeatData := m.prepareHeartbeatData()
	if len(heartbeatData.ConsumerDataSet) == 0 {
		return errors.New("send heartbeat error")
	}
	m.mqClient.sendHeartbeatToAllBroker(heartbeatData)
	return nil
}

//routeInfo
func (m *MqClientManager) updateTopicRouteInfoFromNameServer() {
	var topicSet []string
	for _, consumer := range m.clientFactory.consumerTable {
		for key := range consumer.subscription {
			topicSet = append(topicSet, key)
		}
	}
	topicSet = append(topicSet, m.mqClient.getPublishTopicList()...)
	for _, topic := range topicSet {
		m.mqClient.updateTopicRouteInfoFromNameServer(topic)

	}
}

func (m *MqClientManager) prepareHeartbeatData() *model.HeartbeatData {
	heartbeatData := new(model.HeartbeatData)
	heartbeatData.ClientId = m.mqClient.getClientId()
	heartbeatData.ConsumerDataSet = make([]*model.ConsumerData, 0)
	heartbeatData.ProducerDataSet = make([]*model.ProducerData, 0)
	for group, consumer := range m.clientFactory.consumerTable {
		consumerData := new(model.ConsumerData)
		consumerData.GroupName = group
		consumerData.ConsumeType = consumer.consumeType
		consumerData.ConsumeFromWhere = consumer.ConsumerConfig.ConsumeFromWhere
		consumerData.MessageModel = consumer.messageModel
		consumerData.SubscriptionDataSet = consumer.Subscriptions()
		consumerData.UnitMode = consumer.unitMode
		heartbeatData.ConsumerDataSet = append(heartbeatData.ConsumerDataSet, consumerData)
	}
	for group := range m.clientFactory.producerTable {
		producerData := new(model.ProducerData)
		producerData.GroupName = group
		heartbeatData.ProducerDataSet = append(heartbeatData.ProducerDataSet, producerData)
	}
	return heartbeatData
}
