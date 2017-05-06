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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/golang/glog"
	"os"
	"strconv"
	"sync"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
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
	ConsumeType() ConsumeType
	RemoveDirtyOffset(mq *message.MessageQueue)
	ComputePullFromWhere(mq *message.MessageQueue)
	DispatchPullRequest(pullRequests []model.PullResult)
	RemoveProcessQueue(mq message.MessageQueue)
	ProcessQueueTable() map[message.MessageQueue]*model.ProcessQueue
	TopicSubscribeInfoTable() map[string][]*message.MessageQueue
	ConsumerGroup() string
	SetConsumerGroup(group string)
	MessageModel() MessageModel
	Strategy() AllocateMessageQueueStrategy
	Destroy()
}

type rebalance struct {
	processQueueTable map[*message.MessageQueue]*model.ProcessQueue
	processQueueTableMu sync.RWMutex
	topicSubscribeInfoTable util.ConcurrentMap //map[string]util.Set
	subscriptions  util.ConcurrentMap //map[string]model.SubscriptionData
	groupName string
	messageModel MessageModel
	allocator AllocateMessageQueueStrategy
	mqClient *MQClient
}

func (r *rebalance) Lock(mq *message.MessageQueue) bool {
	findBrokerResult := r.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName(), 0, true)//mixall
	if findBrokerResult != nil {
		requestBody := model.LockBatchRequestBody{
			ConsumerGroup: r.ConsumerGroup(),
			ClientID: r.mqClient.clientID,
		}

		requestBody.MqSet = util.NewSet()
		requestBody.MqSet.Add(mq)

		lockedMQ, err := r.mqClient.api.LockBatchMQ(findBrokerResult.BrokerAddress, &requestBody, time.Second)

		if err != nil {
			glog.Error(err.Error())
			return false
		}

		for _, v := range lockedMQ.Flatten() {
			processQueue := r.processQueueTable[&v.(message.MessageQueue)]
			if processQueue != nil {
				// todo
			}
		}

		lockOK := lockedMQ.Exists(mq)

		glog.Infof("the message queue lock %s, %s, %s", lockOK, r.ConsumerGroup(), mq)

		return lockOK
	}
	return false
}

func (r *rebalance) UnLock(mq *message.MessageQueue, oneWay bool) {
	findBrokerResult := r.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName(), 0, true) // MixAll

	if findBrokerResult != nil {
		requestBody := model.UnlockBatchRequestBody{
			ConsumerGroup: r.groupName,
			ClientID: r.mqClient.clientID,
		}
		requestBody.MqSet = util.NewSet()
		requestBody.MqSet.Add(mq)

		err := r.mqClient.api.UnlockBatchMQ(findBrokerResult.BrokerAddress, &requestBody, time.Second, oneWay)
		glog.Warningf("unlock messageQueue . group:%s, clientID:%s, mq: %s",
			r.ConsumerGroup(), r.mqClient.clientID, mq)
		if err != nil {
			glog.Error(err.Error())
		}
	}
}

func (r *rebalance) LockAll(oneWay bool) {
	brokerMQs := r.buildProcessQueueTableByBrokerName()

	for name, mqs := range brokerMQs {

		if mqs.Len() == 0 {
			continue
		}

		findBrokerResult := r.mqClient.FindBrokerAddressInSubscribe(name, 0, true)//mixall
		if findBrokerResult != nil {
			requestBody := model.LockBatchRequestBody{
				ConsumerGroup: r.ConsumerGroup(),
				ClientID:      r.mqClient.clientID,
			}

			requestBody.MqSet = &mqs

			lockOKMQSet, err := r.mqClient.api.LockBatchMQ(findBrokerResult.BrokerAddress, &requestBody, time.Second)

			if err != nil {
				glog.Error(err.Error())
				return
			}
			for _, mq := range lockOKMQSet.Flatten() {
				processQueue := r.processQueueTable[mq.(*message.MessageQueue)]
				if processQueue != nil {
					// TODO
				}
			}

			for _, mq := range mqs.Flatten() {
				if !lockOKMQSet.Exists(mq) {
					processQueue := r.processQueueTable[mq.(*message.MessageQueue)]
					if processQueue != nil {
						// TODO
					}
				}
			}
		}
	}
}

func (r *rebalance) UnLockAll() {
	brokerMQs := r.buildProcessQueueTableByBrokerName()

	for name, mqs := range brokerMQs {

		if mqs.Len() == 0 {
			continue
		}

		findBrokerResult := r.mqClient.FindBrokerAddressInSubscribe(name, 0, true)//mixall
		if findBrokerResult != nil {
			requestBody := model.UnlockBatchRequestBody{
				ConsumerGroup: r.ConsumerGroup(),
				ClientID:      r.mqClient.clientID,
			}

			requestBody.MqSet = &mqs

			err := r.mqClient.api.UnlockBatchMQ(findBrokerResult.BrokerAddress, &requestBody, time.Second, false)

			if err != nil {
				glog.Error(err.Error())
				return
			}

			for _, mq := range mqs.Flatten() {
				processQueue := r.processQueueTable[mq.(*message.MessageQueue)]
				if processQueue != nil {
					// TODO
				}
			}
		}
	}
}

func (r *rebalance) DoRebalance(ordered bool)
func (r *rebalance) SubscriptionInner() map[string]model.SubscriptionData
func (r *rebalance) buildProcessQueueTableByBrokerName() map[string]util.Set
func (r *rebalance) rebalanceByTopic(topic string, ordered bool)
func (r *rebalance) truncateMessageQueueNotMyTopic()
func (r *rebalance) updateProcessQueueTableInRebalance(topic string,
	mqSet []*message.MessageQueue, ordered bool) bool
func (r *rebalance) RemoveProcessQueue(mq message.MessageQueue)
func (r *rebalance) ProcessQueueTable() map[message.MessageQueue]*model.ProcessQueue
func (r *rebalance) TopicSubscribeInfoTable() map[string][]*message.MessageQueue
func (r *rebalance) ConsumerGroup() string
func (r *rebalance) SetConsumerGroup(group string)
func (r *rebalance) MessageModel() MessageModel
func (r *rebalance) Strategy() AllocateMessageQueueStrategy
func (r *rebalance) Destroy()

type PullMessageRebalance struct {
	rebalance
}

func (pmr *PullMessageRebalance) MessageQueueChanged(topic string, mqAll, mqDivided []*message.MessageQueue)
func (pmr *PullMessageRebalance) RemoveUnnecessaryMessageQueue(mq *message.MessageQueue, pq *model.ProcessQueue)
func (pmr *PullMessageRebalance) ConsumeType() ConsumeType
func (pmr *PullMessageRebalance) RemoveDirtyOffset(mq *message.MessageQueue)
func (pmr *PullMessageRebalance) ComputePullFromWhere(mq *message.MessageQueue)
func (pmr *PullMessageRebalance) DispatchPullRequest(pullRequests []model.PullResult)

type PushMessageRebalance struct {
	rebalance
}

func (pmr *PushMessageRebalance) MessageQueueChanged(topic string, mqAll, mqDivided []*message.MessageQueue)
func (pmr *PushMessageRebalance) RemoveUnnecessaryMessageQueue(mq *message.MessageQueue, pq *model.ProcessQueue)
func (pmr *PushMessageRebalance) ConsumeType() ConsumeType
func (pmr *PushMessageRebalance) RemoveDirtyOffset(mq *message.MessageQueue)
func (pmr *PushMessageRebalance) ComputePullFromWhere(mq *message.MessageQueue)
func (pmr *PushMessageRebalance) DispatchPullRequest(pullRequests []model.PullResult)

type rBScheduler struct { // Rebalance Service Scheduler
	mqClient *MQClient
	quit     chan bool
	quitOnce sync.Once
}

func (rb *rBScheduler) Start() {
	go rb.run()
	glog.Info("RocketMQ Client Rebalance Service STARTED!")
}

func (rb *rBScheduler) Shutdown() {
	rb.quitOnce.Do(func() {
		rb.quit <- true
		glog.Info("RocketMQ Client Rebalance Service SHUTDOWN!")
	})
}

func (rb *rBScheduler) run() {
	timer := time.NewTimer(waitInterval)
	for {
		select {
		case timer.C:
			rb.mqClient.DoRebalance()
		case <-rb.quit:
			return
		}
	}
}
