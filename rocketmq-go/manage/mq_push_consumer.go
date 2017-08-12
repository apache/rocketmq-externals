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
package rocketmq

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"strings"
	"time"
)

type DefaultMQPushConsumer struct {
	consumerGroup string
	//consumeFromWhere      string
	consumeType  string
	messageModel string
	unitMode     bool

	subscription          map[string]string   //topic|subExpression
	subscriptionTag       map[string][]string // we use it filter again
	offsetStore           service.OffsetStore
	mqClient              service.RocketMqClient
	rebalance             *service.Rebalance
	pause                 bool //when reset offset we need pause
	consumeMessageService service.ConsumeMessageService
	ConsumerConfig        *rocketmq_api_model.RocketMqConsumerConfig
}

func NewDefaultMQPushConsumer(consumerGroup string, consumerConfig *rocketmq_api_model.RocketMqConsumerConfig) (defaultMQPushConsumer *DefaultMQPushConsumer) {
	defaultMQPushConsumer = &DefaultMQPushConsumer{
		consumerGroup: consumerGroup,
		//consumeFromWhere:"CONSUME_FROM_FIRST_OFFSET", //todo  use config
		consumeType:  "CONSUME_PASSIVELY",
		messageModel: "CLUSTERING",
		pause:        false}
	defaultMQPushConsumer.subscription = make(map[string]string)
	defaultMQPushConsumer.subscriptionTag = make(map[string][]string)
	defaultMQPushConsumer.ConsumerConfig = consumerConfig

	return
}
func (self *DefaultMQPushConsumer) Subscribe(topic string, subExpression string) {
	self.subscription[topic] = subExpression
	if len(subExpression) == 0 || subExpression == "*" {
		return
	}
	tags := strings.Split(subExpression, "||")
	tagsList := []string{}
	for _, tag := range tags {
		t := strings.TrimSpace(tag)
		if len(t) == 0 {
			continue
		}
		tagsList = append(tagsList, t)
	}
	if len(tagsList) > 0 {
		self.subscriptionTag[topic] = tagsList
	}
}

func (self *DefaultMQPushConsumer) RegisterMessageListener(messageListener model.MessageListener) {
	self.consumeMessageService = service.NewConsumeMessageConcurrentlyServiceImpl(messageListener)
}

func (self *DefaultMQPushConsumer) resetOffset(offsetTable map[model.MessageQueue]int64) {
	self.pause = true
	glog.Info("now we ClearProcessQueue 0 ", offsetTable)

	self.rebalance.ClearProcessQueue(offsetTable)
	glog.Info("now we ClearProcessQueue", offsetTable)
	go func() {
		waitTime := time.NewTimer(10 * time.Second)
		<-waitTime.C
		defer func() {
			self.pause = false
			self.rebalance.DoRebalance()
		}()

		for messageQueue, offset := range offsetTable {
			processQueue := self.rebalance.GetProcessQueue(messageQueue)
			if processQueue == nil || offset < 0 {
				continue
			}
			glog.Info("now we UpdateOffset", messageQueue, offset)
			self.offsetStore.UpdateOffset(&messageQueue, offset, false)
			self.rebalance.RemoveProcessQueue(&messageQueue)
		}
	}()
}

func (self *DefaultMQPushConsumer) Subscriptions() []*model.SubscriptionData {
	subscriptions := make([]*model.SubscriptionData, 0)
	for _, subscription := range self.rebalance.SubscriptionInner {
		subscriptions = append(subscriptions, subscription)
	}
	return subscriptions
}

func (self *DefaultMQPushConsumer) CleanExpireMsg() {
	nowTime := util.CurrentTimeMillisInt64() //will cause nowTime - consumeStartTime <0 ,but no matter
	messageQueueList, processQueueList := self.rebalance.GetProcessQueueList()
	for messageQueueIndex, processQueue := range processQueueList {
		loop := processQueue.GetMsgCount()
		if loop > 16 {
			loop = 16
		}
		for i := 0; i < loop; i++ {
			_, message := processQueue.GetMinMessageInTree()
			if message == nil {
				break
			}
			consumeStartTime := message.GetConsumeStartTime()
			maxDiffTime := self.ConsumerConfig.ConsumeTimeout * 1000 * 60
			//maxDiffTime := self.ConsumerConfig.ConsumeTimeout
			glog.V(2).Info("look message.GetConsumeStartTime()", consumeStartTime)
			glog.V(2).Infof("look diff %d  %d", nowTime-consumeStartTime, maxDiffTime)
			//if(nowTime - consumeStartTime <0){
			//	panic("nowTime - consumeStartTime <0")
			//}
			if nowTime-consumeStartTime < maxDiffTime {
				break
			}
			glog.Info("look now we send expire message back", message.Topic, message.MsgId)
			err := self.consumeMessageService.SendMessageBack(message, 3, messageQueueList[messageQueueIndex].BrokerName)
			if err != nil {
				glog.Error("op=send_expire_message_back_error", err)
				continue
			}
			processQueue.DeleteExpireMsg(int(message.QueueOffset))
		}
	}
	return
}
