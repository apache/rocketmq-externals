/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package service

import (
	"encoding/json"
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
)

type SendMessageBackProducerService interface {
	SendMessageBack(messageExt *rocketmq_api_model.MessageExt, delayLayLevel int, brokerName string) (err error)
	InitSendMessageBackProducerService(consumerGroup string, mqClient RocketMqClient, defaultProducerService *DefaultProducerService, consumerConfig *rocketmq_api_model.RocketMqConsumerConfig)
}

type SendMessageBackProducerServiceImpl struct {
	mqClient               RocketMqClient
	defaultProducerService *DefaultProducerService // one namesvr only one
	consumerGroup          string
	consumerConfig         *rocketmq_api_model.RocketMqConsumerConfig //one mq group have one
}

// send to original broker,if fail send a new retry message
func (self *SendMessageBackProducerServiceImpl) SendMessageBack(messageExt *rocketmq_api_model.MessageExt, delayLayLevel int, brokerName string) (err error) {
	glog.V(2).Info("op=look_send_message_back", messageExt.MsgId, messageExt.Properties, string(messageExt.Body))
	err = self.consumerSendMessageBack(brokerName, messageExt, delayLayLevel) // todo use
	if err == nil {
		return
	}
	glog.Error(err)
	err = self.sendRetryMessageBack(messageExt)
	return
}

func (self *SendMessageBackProducerServiceImpl) sendRetryMessageBack(messageExt *rocketmq_api_model.MessageExt) error {
	// todo build a retry topic todo check todo check
	retryMessage := &rocketmq_api_model.Message{}
	originMessageId := messageExt.GetOriginMessageId()
	retryMessage.Properties = messageExt.Properties
	retryMessage.SetOriginMessageId(originMessageId)
	retryMessage.Flag = messageExt.Flag
	retryMessage.Topic = constant.RETRY_GROUP_TOPIC_PREFIX + self.consumerGroup
	retryMessage.Body = messageExt.Body
	retryMessage.SetRetryTopic(messageExt.Topic)
	retryMessage.SetReconsumeTime(messageExt.GetReconsumeTimes() + 1)
	retryMessage.SetMaxReconsumeTimes(self.consumerConfig.MaxReconsumeTimes)
	retryMessage.SetDelayTimeLevel(3 + messageExt.GetReconsumeTimes())
	pp, _ := json.Marshal(retryMessage)
	glog.Info("look retryMessage ", string(pp), string(messageExt.Body))
	sendResult, err := self.defaultProducerService.SendDefaultImpl(retryMessage, constant.COMMUNICATIONMODE_SYNC, "", self.defaultProducerService.producerConfig.SendMsgTimeout)
	if err != nil {
		glog.Error(err)
		return err
	}
	xx, _ := json.Marshal(sendResult)
	glog.Info("look retryMessage result", string(xx))
	// todo need check send result
	return nil

}

func (self *SendMessageBackProducerServiceImpl) InitSendMessageBackProducerService(consumerGroup string, mqClient RocketMqClient, defaultProducerService *DefaultProducerService, consumerConfig *rocketmq_api_model.RocketMqConsumerConfig) {
	self.mqClient = mqClient
	self.consumerGroup = consumerGroup
	self.defaultProducerService = defaultProducerService
	self.consumerConfig = consumerConfig
}

func (self *SendMessageBackProducerServiceImpl) consumerSendMessageBack(brokerName string, messageExt *rocketmq_api_model.MessageExt, delayLayLevel int) (err error) {
	if len(brokerName) == 0 {
		err = errors.New("broker can't be empty")
		glog.Error(err)
		return
	}
	brokerAddr := self.mqClient.FetchMasterBrokerAddress(brokerName)
	sendMsgBackHeader := &header.ConsumerSendMsgBackRequestHeader{
		Offset:            messageExt.CommitLogOffset,
		Group:             self.consumerGroup,
		DelayLevel:        0, //Message consume retry strategy<br>-1,no retry,put into DLQ directly<br>0,broker control retry frequency<br>>0,client control retry frequency
		OriginMsgId:       messageExt.MsgId,
		OriginTopic:       messageExt.Topic,
		UnitMode:          false,
		MaxReconsumeTimes: int32(self.consumerConfig.MaxReconsumeTimes),
	}
	remotingCommand := remoting.NewRemotingCommand(remoting.CONSUMER_SEND_MSG_BACK, sendMsgBackHeader)
	response, invokeErr := self.mqClient.GetRemotingClient().InvokeSync(brokerAddr, remotingCommand, 5000)
	if invokeErr != nil {
		err = invokeErr
		return
	}
	if response == nil || response.Code != remoting.SUCCESS {
		glog.Error("sendMsgBackRemarkError", response.Remark)
		err = errors.New("send Message back error")
	}
	return
}
