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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
)

type sendMessageBackProducerService interface {
	sendMessageBack(messageExt *message.MessageExtImpl, delayLayLevel int, brokerName string) (err error)
	initSendMessageBackProducerService(consumerGroup string, mqClient RocketMqClient, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig)
}

type sendMessageBackProducerServiceImpl struct {
	mqClient               RocketMqClient
	defaultProducerService *DefaultProducerService // one namesvr only one
	consumerGroup          string
	consumerConfig         *rocketmqm.MqConsumerConfig //one mq group have one
}

// send to original broker,if fail send a new retry message
func (s *sendMessageBackProducerServiceImpl) sendMessageBack(messageExt *message.MessageExtImpl, delayLayLevel int, brokerName string) (err error) {
	glog.V(2).Info("op=look_send_message_back", messageExt.MsgId(), messageExt.Properties(), string(messageExt.Body()))
	err = s.consumerSendMessageBack(brokerName, messageExt, delayLayLevel)
	if err == nil {
		return
	}
	glog.Error(err)
	err = s.sendRetryMessageBack(messageExt)
	return
}

func (s *sendMessageBackProducerServiceImpl) sendRetryMessageBack(messageExt *message.MessageExtImpl) error {
	retryMessage := &message.MessageImpl{}
	originMessageId := messageExt.GetOriginMessageId()
	retryMessage.SetProperties(messageExt.Properties())
	retryMessage.SetOriginMessageId(originMessageId)
	retryMessage.SetFlag(messageExt.Flag())
	retryMessage.SetTopic(constant.RETRY_GROUP_TOPIC_PREFIX + s.consumerGroup)
	retryMessage.SetBody(messageExt.Body())
	retryMessage.SetRetryTopic(messageExt.Topic())
	retryMessage.SetReconsumeTime(messageExt.GetReconsumeTimes() + 1)
	retryMessage.SetMaxReconsumeTimes(s.consumerConfig.MaxReconsumeTimes)
	retryMessage.SetDelayTimeLevel(3 + messageExt.GetReconsumeTimes())
	pp, _ := json.Marshal(retryMessage)
	glog.Info("look retryMessage ", string(pp), string(messageExt.Body()))
	sendResult, err := s.defaultProducerService.sendDefaultImpl(retryMessage, constant.COMMUNICATIONMODE_SYNC, "", s.defaultProducerService.producerConfig.SendMsgTimeout)
	if err != nil {
		glog.Error(err)
		return err
	}
	xx, _ := json.Marshal(sendResult)
	glog.V(2).Info("look retryMessage result", string(xx))
	return nil

}

func (s *sendMessageBackProducerServiceImpl) initSendMessageBackProducerService(consumerGroup string, mqClient RocketMqClient, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig) {
	s.mqClient = mqClient
	s.consumerGroup = consumerGroup
	s.defaultProducerService = defaultProducerService
	s.consumerConfig = consumerConfig
}

func (s *sendMessageBackProducerServiceImpl) consumerSendMessageBack(brokerName string, messageExt *message.MessageExtImpl, delayLayLevel int) (err error) {
	if len(brokerName) == 0 {
		err = errors.New("broker can't be empty")
		glog.Error(err)
		return
	}
	brokerAddr := s.mqClient.fetchMasterBrokerAddress(brokerName)
	sendMsgBackHeader := &header.ConsumerSendMsgBackRequestHeader{
		Offset:            messageExt.CommitLogOffset,
		Group:             s.consumerGroup,
		DelayLevel:        0, //MessageImpl consume retry strategy<br>-1,no retry,put into DLQ directly<br>0,broker control retry frequency<br>>0,client control retry frequency
		OriginMsgId:       messageExt.MsgId(),
		OriginTopic:       messageExt.Topic(),
		UnitMode:          false,
		MaxReconsumeTimes: int32(s.consumerConfig.MaxReconsumeTimes),
	}
	remotingCommand := remoting.NewRemotingCommand(remoting.CONSUMER_SEND_MSG_BACK, sendMsgBackHeader)
	response, invokeErr := s.mqClient.getRemotingClient().InvokeSync(brokerAddr, remotingCommand, 5000)
	if invokeErr != nil {
		err = invokeErr
		return
	}
	if response == nil || response.Code != remoting.SUCCESS {
		glog.Error("sendMsgBackRemarkError", response.Remark)
		err = errors.New("send MessageImpl back error")
	}
	return
}
