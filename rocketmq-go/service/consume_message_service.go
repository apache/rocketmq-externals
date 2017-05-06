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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/golang/glog"
	"time"
)

type ConsumeMessageService interface {
	//ConsumeMessageDirectlyResult consumeMessageDirectly(final MessageExt msg, final String brokerName);

	Init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *config.RocketMqConsumerConfig)
	SubmitConsumeRequest(msgs []model.MessageExt, processQueue *model.ProcessQueue,
		messageQueue *model.MessageQueue, dispathToConsume bool)
	SendMessageBack(messageExt *model.MessageExt, delayLayLevel int, brokerName string) (err error)
	ConsumeMessageDirectly(messageExt *model.MessageExt, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error)
}

type ConsumeMessageConcurrentlyServiceImpl struct {
	consumerGroup   string
	messageListener model.MessageListener
	//sendMessageBackProducerService SendMessageBackProducerService //for send retry Message
	offsetStore    OffsetStore
	consumerConfig *config.RocketMqConsumerConfig
}

func NewConsumeMessageConcurrentlyServiceImpl(messageListener model.MessageListener) (consumeService ConsumeMessageService) {
	//consumeService = &ConsumeMessageConcurrentlyServiceImpl{messageListener:messageListener, sendMessageBackProducerService:&SendMessageBackProducerServiceImpl{}}
	return
}

func (self *ConsumeMessageConcurrentlyServiceImpl) Init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *config.RocketMqConsumerConfig) {
	self.consumerGroup = consumerGroup
	self.offsetStore = offsetStore
	//self.sendMessageBackProducerService.InitSendMessageBackProducerService(consumerGroup, mqClient,defaultProducerService,consumerConfig)
	self.consumerConfig = consumerConfig
}

func (self *ConsumeMessageConcurrentlyServiceImpl) SubmitConsumeRequest(msgs []model.MessageExt, processQueue *model.ProcessQueue, messageQueue *model.MessageQueue, dispathToConsume bool) {
	msgsLen := len(msgs)
	for i := 0; i < msgsLen; {
		begin := i
		end := i + self.consumerConfig.ConsumeMessageBatchMaxSize
		if end > msgsLen {
			end = msgsLen
		}
		go func() {
			glog.V(2).Infof("look slice begin %d end %d msgsLen %d", begin, end, msgsLen)
			batchMsgs := transformMessageToConsume(self.consumerGroup, msgs[begin:end])
			consumeState := self.messageListener(batchMsgs)
			self.processConsumeResult(consumeState, batchMsgs, messageQueue, processQueue)
		}()
		i = end
	}
	return
}

func (self *ConsumeMessageConcurrentlyServiceImpl) SendMessageBack(messageExt *model.MessageExt, delayLayLevel int, brokerName string) (err error) {
	//err = self.sendMessageBackProducerService.SendMessageBack(messageExt, 0, brokerName)
	return
}

func (self *ConsumeMessageConcurrentlyServiceImpl) ConsumeMessageDirectly(messageExt *model.MessageExt, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error) {
	start := time.Now().UnixNano() / 1000000
	consumeResult := self.messageListener([]model.MessageExt{*messageExt})
	consumeMessageDirectlyResult.AutoCommit = true
	consumeMessageDirectlyResult.Order = false
	consumeMessageDirectlyResult.SpentTimeMills = time.Now().UnixNano()/1000000 - start
	if consumeResult.ConsumeConcurrentlyStatus == "CONSUME_SUCCESS" && consumeResult.AckIndex >= 0 {
		consumeMessageDirectlyResult.ConsumeResult = "CR_SUCCESS"

	} else {
		consumeMessageDirectlyResult.ConsumeResult = "CR_THROW_EXCEPTION"
	}
	return
}

func (self *ConsumeMessageConcurrentlyServiceImpl) processConsumeResult(result model.ConsumeConcurrentlyResult, msgs []model.MessageExt, messageQueue *model.MessageQueue, processQueue *model.ProcessQueue) {
	if processQueue.IsDropped() {
		glog.Warning("processQueue is dropped without process consume result. ", msgs)
		return
	}
	if len(msgs) == 0 {
		return
	}
	ackIndex := result.AckIndex
	if model.CONSUME_SUCCESS == result.ConsumeConcurrentlyStatus {
		if ackIndex >= len(msgs) {
			ackIndex = len(msgs) - 1
		} else {
			if result.AckIndex < 0 {
				ackIndex = -1
			}
		}
	}
	var failedMessages []model.MessageExt
	successMessages := []model.MessageExt{}
	if ackIndex >= 0 {
		successMessages = msgs[:ackIndex+1]
	}
	for i := ackIndex + 1; i < len(msgs); i++ {
		err := self.SendMessageBack(&msgs[i], 0, messageQueue.BrokerName)
		if err != nil {
			msgs[i].ReconsumeTimes = msgs[i].ReconsumeTimes + 1
			failedMessages = append(failedMessages, msgs[i])
		} else {
			successMessages = append(successMessages, msgs[i])
		}
	}
	if len(failedMessages) > 0 {
		self.SubmitConsumeRequest(failedMessages, processQueue, messageQueue, true)
	}
	//commitOffset := processQueue.RemoveMessage(successMessages)
	//if (commitOffset > 0 && ! processQueue.IsDropped()) {
	//	self.offsetStore.UpdateOffset(messageQueue, commitOffset, true)
	//}

}

func transformMessageToConsume(consumerGroup string, msgs []model.MessageExt) []model.MessageExt {
	retryTopicName := constant.RETRY_GROUP_TOPIC_PREFIX + consumerGroup

	for _, msg := range msgs {
		//reset retry topic name
		if msg.Message.Topic == retryTopicName {
			retryTopic := msg.Properties[constant.PROPERTY_RETRY_TOPIC]
			if len(retryTopic) > 0 {
				msg.Message.Topic = retryTopic
			}
		}
		//set consume start time
		msg.SetConsumeStartTime()
	}
	return msgs
}
