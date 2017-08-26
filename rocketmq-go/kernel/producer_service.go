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
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
)

//ProducerService producerService, for send message
type ProducerService interface {
	checkConfig() (err error)
	sendDefaultImpl(message *message.MessageImpl, communicationMode string, sendCallback string, timeout int64) (sendResult *rocketmqm.SendResult, err error)
}

//DefaultProducerService ProducerService's implement
type DefaultProducerService struct {
	producerGroup   string
	producerConfig  *rocketmqm.MqProducerConfig
	mqClient        RocketMqClient
	mqFaultStrategy mqFaultStrategy
}

func newDefaultProducerService(producerGroup string, producerConfig *rocketmqm.MqProducerConfig, mqClient RocketMqClient) (defaultProducerService *DefaultProducerService) {
	defaultProducerService = &DefaultProducerService{
		mqClient:       mqClient,
		producerGroup:  producerGroup,
		producerConfig: producerConfig,
	}
	defaultProducerService.checkConfig()
	return
}
func (d *DefaultProducerService) checkConfig() (err error) {
	// todo check if not pass panic
	return
}

func (d *DefaultProducerService) sendDefaultImpl(message *message.MessageImpl, communicationMode string, sendCallback string, timeout int64) (sendResult *rocketmqm.SendResult, err error) {
	var (
		topicPublishInfo *model.TopicPublishInfo
	)
	err = d.checkMessage(message)
	if err != nil {
		return
	}
	topicPublishInfo, err = d.mqClient.tryToFindTopicPublishInfo(message.Topic())
	if err != nil {
		return
	}
	if topicPublishInfo.JudgeTopicPublishInfoOk() == false {
		err = errors.New("topicPublishInfo is error,topic=" + message.Topic())
		return
	}
	glog.V(2).Info("op=look topicPublishInfo", topicPublishInfo)
	//if(!ok) return error
	sendResult, err = d.sendMsgUseTopicPublishInfo(message, communicationMode, sendCallback, topicPublishInfo, timeout)
	return
}

func (d *DefaultProducerService) producerSendMessageRequest(brokerName, brokerAddr string, sendMessageHeader remoting.CustomerHeader, message *message.MessageImpl, timeout int64) (sendResult *rocketmqm.SendResult, err error) {
	remotingCommand := remoting.NewRemotingCommandWithBody(remoting.SEND_MESSAGE, sendMessageHeader, message.Body())
	var response *remoting.RemotingCommand
	response, err = d.mqClient.getRemotingClient().InvokeSync(brokerAddr, remotingCommand, timeout)
	if err != nil {
		glog.Error(err)
		return
	}

	sendResult, err = processSendResponse(brokerName, message, response)
	return
}
func processSendResponse(brokerName string, message *message.MessageImpl, response *remoting.RemotingCommand) (sendResult *rocketmqm.SendResult, err error) {
	sendResult = &rocketmqm.SendResult{}
	switch response.Code {
	case remoting.FLUSH_DISK_TIMEOUT:
		{
			sendResult.SetSendStatus(rocketmqm.FlushDiskTimeout)
			break
		}
	case remoting.FLUSH_SLAVE_TIMEOUT:
		{
			sendResult.SetSendStatus(rocketmqm.FlushSlaveTimeout)
			break
		}
	case remoting.SLAVE_NOT_AVAILABLE:
		{
			sendResult.SetSendStatus(rocketmqm.SlaveNotAvaliable)
			break
		}
	case remoting.SUCCESS:
		{
			sendResult.SetSendStatus(rocketmqm.SendOK)
			break
		}
	default:
		err = errors.New("response.Code error_code=" + util.IntToString(int(response.Code)))
		return
	}
	var responseHeader = &header.SendMessageResponseHeader{}
	if response.ExtFields != nil {
		responseHeader.FromMap(response.ExtFields) //change map[string]interface{} into CustomerHeader struct
	}
	sendResult.SetMsgID(message.PropertiesKeyValue(constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX))
	sendResult.SetOffsetMsgID(responseHeader.MsgId)
	sendResult.SetQueueOffset(responseHeader.QueueOffset)
	sendResult.SetTransactionID(responseHeader.TransactionId)
	messageQueue := rocketmqm.MessageQueue{Topic: message.Topic(), BrokerName: brokerName,
		QueueId: responseHeader.QueueId}
	sendResult.SetMessageQueue(messageQueue)
	var regionId = responseHeader.MsgRegion
	if len(regionId) == 0 {
		regionId = "DefaultRegion"
	}
	sendResult.SetRegionID(regionId)
	return
}

func (d *DefaultProducerService) checkMessage(message *message.MessageImpl) (err error) {
	if message == nil {
		err = errors.New("message is nil")
		return
	}
	if len(message.Topic()) == 0 {
		err = errors.New("topic is empty")
		return
	}
	if message.Topic() == constant.DEFAULT_TOPIC {
		err = errors.New("the topic[" + message.Topic() + "] is conflict with default topic.")
		return
	}

	if len(message.Topic()) > constant.MAX_MESSAGE_TOPIC_SIZE {
		err = errors.New("the specified topic is longer than topic max length 255.")
		return
	}

	if !util.MatchString(message.Topic(), `^[%|a-zA-Z0-9_-]+$`) {
		err = errors.New("the specified topic[" + message.Topic() + "] contains illegal characters")
		return
	}
	if len(message.Body()) == 0 {
		err = errors.New("messageBody is empty")
		return
	}
	if len(message.Body()) > d.producerConfig.MaxMessageSize {
		err = errors.New("messageBody is large than " + util.IntToString(d.producerConfig.MaxMessageSize))
		return
	}
	return
}

func (d *DefaultProducerService) sendMsgUseTopicPublishInfo(message *message.MessageImpl, communicationMode string, sendCallback string, topicPublishInfo *model.TopicPublishInfo, timeout int64) (sendResult *rocketmqm.SendResult, err error) {
	var (
		sendTotalTime int
		messageQueue  rocketmqm.MessageQueue
	)

	sendTotalTime = 1
	var lastFailedBroker = ""
	//todo transaction
	// todo retry
	for i := 0; i < sendTotalTime; i++ {
		messageQueue, err = selectOneMessageQueue(topicPublishInfo, lastFailedBroker)
		if err != nil {
			return
		}
		sendResult, err = d.doSendMessage(message, messageQueue, communicationMode, sendCallback, topicPublishInfo, timeout)
		if err != nil {
			// todo retry
			return
		}
	}
	return
}

func (d *DefaultProducerService) doSendMessage(message *message.MessageImpl, messageQueue rocketmqm.MessageQueue,
	communicationMode string, sendCallback string,
	topicPublishInfo *model.TopicPublishInfo,
	timeout int64) (sendResult *rocketmqm.SendResult, err error) {
	var (
		brokerAddr          string
		sysFlag             int
		compressMessageFlag int
	)
	compressMessageFlag, err = d.tryToCompressMessage(message)
	if err != nil {
		return
	}
	sysFlag = sysFlag | compressMessageFlag
	brokerAddr = d.mqClient.fetchMasterBrokerAddress(messageQueue.BrokerName)
	if len(brokerAddr) == 0 {
		err = errors.New("The broker[" + messageQueue.BrokerName + "] not exist")
		return
	}
	message.GeneratorMsgUniqueKey()
	sendMessageHeader := &header.SendMessageRequestHeader{
		ProducerGroup:         d.producerGroup,
		Topic:                 message.Topic(),
		DefaultTopic:          constant.DEFAULT_TOPIC,
		DefaultTopicQueueNums: 4,
		QueueId:               messageQueue.QueueId,
		SysFlag:               sysFlag,
		BornTimestamp:         util.CurrentTimeMillisInt64(),
		Flag:                  message.Flag(),
		Properties:            util.MessageProperties2String(message.Properties()),

		UnitMode:          false,
		ReconsumeTimes:    message.GetReconsumeTimes(),
		MaxReconsumeTimes: message.GetMaxReconsumeTimes(),
	}
	sendResult, err = d.producerSendMessageRequest(messageQueue.BrokerName, brokerAddr, sendMessageHeader, message, timeout)
	return
}

func (d *DefaultProducerService) tryToCompressMessage(message *message.MessageImpl) (compressedFlag int, err error) {
	if len(message.Body()) < d.producerConfig.CompressMsgBodyOverHowMuch {
		compressedFlag = 0
		return
	}
	compressedFlag = int(constant.CompressedFlag)
	var compressBody []byte
	compressBody, err = util.CompressWithLevel(message.Body(), d.producerConfig.ZipCompressLevel)
	message.SetBody(compressBody)
	return
}
