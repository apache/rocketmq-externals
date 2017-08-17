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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
)

type ConsumeMessageService interface {
	Init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig)
	SubmitConsumeRequest(msgs []rocketmqm.MessageExt, processQueue *model.ProcessQueue,
		messageQueue *model.MessageQueue, dispathToConsume bool)
	SendMessageBack(messageExt *rocketmqm.MessageExt, delayLayLevel int, brokerName string) (err error)
	ConsumeMessageDirectly(messageExt *rocketmqm.MessageExt, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error)
}

type ConsumeMessageConcurrentlyServiceImpl struct {
	consumerGroup                  string
	messageListener                model.MessageListener
	sendMessageBackProducerService SendMessageBackProducerService //for send retry Message
	offsetStore                    OffsetStore
	consumerConfig                 *rocketmqm.MqConsumerConfig
}

func NewConsumeMessageConcurrentlyServiceImpl(messageListener model.MessageListener) (consumeService ConsumeMessageService) {
	consumeService = &ConsumeMessageConcurrentlyServiceImpl{messageListener: messageListener, sendMessageBackProducerService: &SendMessageBackProducerServiceImpl{}}
	return
}

func (c *ConsumeMessageConcurrentlyServiceImpl) Init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig) {
	c.consumerGroup = consumerGroup
	c.offsetStore = offsetStore
	c.sendMessageBackProducerService.InitSendMessageBackProducerService(consumerGroup, mqClient, defaultProducerService, consumerConfig)
	c.consumerConfig = consumerConfig
}

func (c *ConsumeMessageConcurrentlyServiceImpl) SubmitConsumeRequest(msgs []rocketmqm.MessageExt, processQueue *model.ProcessQueue, messageQueue *model.MessageQueue, dispathToConsume bool) {
	msgsLen := len(msgs)
	for i := 0; i < msgsLen; {
		begin := i
		end := i + c.consumerConfig.ConsumeMessageBatchMaxSize
		if end > msgsLen {
			end = msgsLen
		}
		go func() {
			glog.V(2).Infof("look slice begin %d end %d msgsLen %d", begin, end, msgsLen)
			batchMsgs := transformMessageToConsume(c.consumerGroup, msgs[begin:end])
			consumeState := c.messageListener(batchMsgs)
			c.processConsumeResult(consumeState, batchMsgs, messageQueue, processQueue)
		}()
		i = end
	}
	return
}

func (c *ConsumeMessageConcurrentlyServiceImpl) SendMessageBack(messageExt *rocketmqm.MessageExt, delayLayLevel int, brokerName string) (err error) {
	err = c.sendMessageBackProducerService.SendMessageBack(messageExt, 0, brokerName)
	return
}

func (c *ConsumeMessageConcurrentlyServiceImpl) ConsumeMessageDirectly(messageExt *rocketmqm.MessageExt, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error) {
	start := util.CurrentTimeMillisInt64()
	consumeResult := c.messageListener([]rocketmqm.MessageExt{*messageExt})
	consumeMessageDirectlyResult.AutoCommit = true
	consumeMessageDirectlyResult.Order = false
	consumeMessageDirectlyResult.SpentTimeMills = util.CurrentTimeMillisInt64() - start
	if consumeResult.ConsumeConcurrentlyStatus == rocketmqm.CONSUME_SUCCESS && consumeResult.AckIndex >= 0 {
		consumeMessageDirectlyResult.ConsumeResult = model.CR_SUCCESS
	} else {
		consumeMessageDirectlyResult.ConsumeResult = model.CR_THROW_EXCEPTION
	}
	return
}

func (c *ConsumeMessageConcurrentlyServiceImpl) processConsumeResult(result rocketmqm.ConsumeConcurrentlyResult, msgs []rocketmqm.MessageExt, messageQueue *model.MessageQueue, processQueue *model.ProcessQueue) {
	if processQueue.IsDropped() {
		glog.Warning("processQueue is dropped without process consume result. ", msgs)
		return
	}
	if len(msgs) == 0 {
		return
	}
	ackIndex := result.AckIndex
	if rocketmqm.CONSUME_SUCCESS == result.ConsumeConcurrentlyStatus {
		if ackIndex >= len(msgs) {
			ackIndex = len(msgs) - 1
		} else {
			if result.AckIndex < 0 {
				ackIndex = -1
			}
		}
	}
	var failedMessages []rocketmqm.MessageExt
	successMessages := []rocketmqm.MessageExt{}
	if ackIndex >= 0 {
		successMessages = msgs[:ackIndex+1]
	}
	for i := ackIndex + 1; i < len(msgs); i++ {
		err := c.SendMessageBack(&msgs[i], 0, messageQueue.BrokerName)
		if err != nil {
			msgs[i].ReconsumeTimes = msgs[i].ReconsumeTimes + 1
			failedMessages = append(failedMessages, msgs[i])
		} else {
			successMessages = append(successMessages, msgs[i])
		}
	}
	if len(failedMessages) > 0 {
		c.SubmitConsumeRequest(failedMessages, processQueue, messageQueue, true)
	}
	commitOffset := processQueue.RemoveMessage(successMessages)
	if commitOffset > 0 && !processQueue.IsDropped() {
		c.offsetStore.UpdateOffset(messageQueue, commitOffset, true)
	}

}

func transformMessageToConsume(consumerGroup string, msgs []rocketmqm.MessageExt) []rocketmqm.MessageExt {
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
