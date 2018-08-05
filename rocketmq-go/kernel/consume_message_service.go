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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
)

type consumeMessageService interface {
	init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig)
	submitConsumeRequest(msgs []message.MessageExtImpl, processQueue *model.ProcessQueue,
		messageQueue *rocketmqm.MessageQueue, dispathToConsume bool)
	sendMessageBack(messageExt *message.MessageExtImpl, delayLayLevel int, brokerName string) (err error)
	consumeMessageDirectly(messageExt *message.MessageExtImpl, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error)
}

type consumeMessageConcurrentlyServiceImpl struct {
	consumerGroup                  string
	messageListener                rocketmqm.MessageListener
	sendMessageBackProducerService sendMessageBackProducerService //for send retry MessageImpl
	offsetStore                    OffsetStore
	consumerConfig                 *rocketmqm.MqConsumerConfig
}

func newConsumeMessageConcurrentlyServiceImpl(messageListener rocketmqm.MessageListener) (consumeService consumeMessageService) {
	consumeService = &consumeMessageConcurrentlyServiceImpl{messageListener: messageListener, sendMessageBackProducerService: &sendMessageBackProducerServiceImpl{}}
	return
}

func (c *consumeMessageConcurrentlyServiceImpl) init(consumerGroup string, mqClient RocketMqClient, offsetStore OffsetStore, defaultProducerService *DefaultProducerService, consumerConfig *rocketmqm.MqConsumerConfig) {
	c.consumerGroup = consumerGroup
	c.offsetStore = offsetStore
	c.sendMessageBackProducerService.initSendMessageBackProducerService(consumerGroup, mqClient, defaultProducerService, consumerConfig)
	c.consumerConfig = consumerConfig
}

func (c *consumeMessageConcurrentlyServiceImpl) submitConsumeRequest(msgs []message.MessageExtImpl, processQueue *model.ProcessQueue, messageQueue *rocketmqm.MessageQueue, dispathToConsume bool) {
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

			consumeState := c.messageListener(c.convert2ConsumeType(batchMsgs))
			c.processConsumeResult(consumeState, batchMsgs, messageQueue, processQueue)
		}()
		i = end
	}
	return
}
func (c *consumeMessageConcurrentlyServiceImpl) convert2ConsumeType(msgs []message.MessageExtImpl) (ret []rocketmqm.MessageExt) {
	msgLen := len(msgs)
	ret = make([]rocketmqm.MessageExt, msgLen)

	for i := 0; i < msgLen; i++ {
		ret[i] = rocketmqm.MessageExt(&msgs[i])
	}
	return
}

func (c *consumeMessageConcurrentlyServiceImpl) sendMessageBack(messageExt *message.MessageExtImpl, delayLayLevel int, brokerName string) (err error) {
	err = c.sendMessageBackProducerService.sendMessageBack(messageExt, 0, brokerName)
	return
}

func (c *consumeMessageConcurrentlyServiceImpl) consumeMessageDirectly(messageExt *message.MessageExtImpl, brokerName string) (consumeMessageDirectlyResult model.ConsumeMessageDirectlyResult, err error) {
	start := util.CurrentTimeMillisInt64()
	consumeResult := c.messageListener(c.convert2ConsumeType([]message.MessageExtImpl{*messageExt}))
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

func (c *consumeMessageConcurrentlyServiceImpl) processConsumeResult(result rocketmqm.ConsumeConcurrentlyResult, msgs []message.MessageExtImpl, messageQueue *rocketmqm.MessageQueue, processQueue *model.ProcessQueue) {
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
	var failedMessages []message.MessageExtImpl
	successMessages := []message.MessageExtImpl{}
	if ackIndex >= 0 {
		successMessages = msgs[:ackIndex+1]
	}
	for i := ackIndex + 1; i < len(msgs); i++ {
		err := c.sendMessageBack(&msgs[i], 0, messageQueue.BrokerName)
		if err != nil {
			msgs[i].ReconsumeTimes = msgs[i].ReconsumeTimes + 1
			failedMessages = append(failedMessages, msgs[i])
		} else {
			successMessages = append(successMessages, msgs[i])
		}
	}
	if len(failedMessages) > 0 {
		c.submitConsumeRequest(failedMessages, processQueue, messageQueue, true)
	}
	commitOffset := processQueue.RemoveMessage(successMessages)
	if commitOffset > 0 && !processQueue.IsDropped() {
		c.offsetStore.updateOffset(messageQueue, commitOffset, true)
	}

}

func transformMessageToConsume(consumerGroup string, msgs []message.MessageExtImpl) []message.MessageExtImpl {
	retryTopicName := constant.RETRY_GROUP_TOPIC_PREFIX + consumerGroup

	for _, msg := range msgs {
		//reset retry topic name
		if msg.MessageImpl.Topic() == retryTopicName {
			retryTopic := msg.PropertiesKeyValue(constant.PROPERTY_RETRY_TOPIC)
			if len(retryTopic) > 0 {
				msg.MessageImpl.SetTopic(retryTopic)
			}
		}
		//set consume start time
		msg.SetConsumeStartTime()
	}
	return msgs
}
