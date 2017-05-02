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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"strconv"
	"os"
	"time"
	"github.com/golang/glog"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/consumer"
)
var waitInterval time.Duration

func init() {
	interval, err := strconv.Atoi(os.Getenv("rocketmq.client.rebalance.waitInterval"))
	if err != nil {
		waitInterval = 20 * time.Second
		glog.Warningf("rocketmq.client.rebalance.waitInterval unset!")
	} else {
		waitInterval = time.Duration(interval) * time.Millisecond
	}
}

type AllocateMessageQueueStrategy interface {
	Allocate(consumeGroup, currentCID string, mqAll []*message.MessageQueue, CIDAll []string) []*message.MessageQueue
	StrategyName() string
}


type Rebalance interface {
	Lock(mq *message.MessageQueue, oneWay bool)
	UnLock(mq *message.MessageQueue)
	LockAll(oneWay bool)
	UnLockAll()
	DoRebalance(ordered bool)
	SubscriptionInner() map[string]model.SubscriptionData
	MessageQueueChanged(topic string, mqAll, mqDivided []*message.MessageQueue)
	RemoveUnnecessaryMessageQueue(mq *message.MessageQueue, pq *model.ProcessQueue)
	ConsumeType() consumer.ConsumeType // TODO cycle dependence
	RemoveDirtyOffset(mq *message.MessageQueue)
	ComputePullFromWhere(mq *message.MessageQueue)
	DispatchPullRequest(pullRequests []model.PullResult)
	RemoveProcessQueue(mq message.MessageQueue)
	ProcessQueueTable() map[message.MessageQueue]*model.ProcessQueue
	TopicSubscribeInfoTable() map[string][]*message.MessageQueue
	ConsumerGroup() string
	SetConsumerGroup(group string)
	MessageModel() consumer.MessageModel // TODO cycle dependence
	Strategy() AllocateMessageQueueStrategy
	Destroy()
}

type commonRebalance struct {

}

func (cr *commonRebalance) Lock(mq *message.MessageQueue, oneWay bool)
func (cr *commonRebalance) UnLock(mq *message.MessageQueue)
func (cr *commonRebalance) LockAll(oneWay bool)
func (cr *commonRebalance) UnLockAll()
func (cr *commonRebalance) DoRebalance(ordered bool)
func (cr *commonRebalance) SubscriptionInner() map[string]model.SubscriptionData
func (cr *commonRebalance) buildProcessQueueTableByBrokerName() map[string][]*message.MessageQueue
func (cr *commonRebalance) rebalanceByTopic(topic string, ordered bool)
func (cr *commonRebalance) truncateMessageQueueNotMyTopic()
func (cr *commonRebalance) updateProcessQueueTableInRebalance(topic string,
	mqSet []*message.MessageQueue, ordered bool) bool
func (cr *commonRebalance) RemoveProcessQueue(mq message.MessageQueue)
func (cr *commonRebalance) ProcessQueueTable() map[message.MessageQueue]*model.ProcessQueue
func (cr *commonRebalance) TopicSubscribeInfoTable() map[string][]*message.MessageQueue
func (cr *commonRebalance) ConsumerGroup() string
func (cr *commonRebalance) SetConsumerGroup(group string)
func (cr *commonRebalance) MessageModel() consumer.MessageModel // TODO cycle dependence
func (cr *commonRebalance) Strategy() AllocateMessageQueueStrategy
func (cr *commonRebalance) Destroy()

type PullMessageRebalance struct {
	commonRebalance
}

func (pmr *PullMessageRebalance) MessageQueueChanged(topic string, mqAll, mqDivided []*message.MessageQueue)
func (pmr *PullMessageRebalance) RemoveUnnecessaryMessageQueue(mq *message.MessageQueue, pq *model.ProcessQueue)
func (pmr *PullMessageRebalance) ConsumeType() consumer.ConsumeType // TODO cycle dependence
func (pmr *PullMessageRebalance) RemoveDirtyOffset(mq *message.MessageQueue)
func (pmr *PullMessageRebalance) ComputePullFromWhere(mq *message.MessageQueue)
func (pmr *PullMessageRebalance) DispatchPullRequest(pullRequests []model.PullResult)

type rBScheduler struct { // Rebalance Service Scheduler
	mqClient *MQClient
	quit chan bool
}

func (rb *rBScheduler) Start() {
	go rb.run()
	glog.Info("RocketMQ Client Rebalance Service STARTED!")
}

func (rb *rBScheduler) Shutdown() {
	rb.quit <- true
	glog.Info("RocketMQ Client Rebalance Service SHUTDOWN!")
}

func (rb *rBScheduler) run() {
	timer := time.NewTimer(waitInterval)
	for {
		select {
		case timer.C:
			rb.mqClient.DoRebalance()
		case <- rb.quit:
			return
		}
	}
}

