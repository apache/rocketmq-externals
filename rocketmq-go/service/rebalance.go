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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service/allocate_message"
	"github.com/golang/glog"
	"sort"
	"strings"
	"sync"
	"time"
)

type Rebalance struct {
	groupName                    string
	messageModel                 string
	topicSubscribeInfoTableLock  sync.RWMutex
	SubscriptionInner            map[string]*model.SubscriptionData
	subscriptionInnerLock        sync.RWMutex
	mqClient                     RocketMqClient
	allocateMessageQueueStrategy service_allocate_message.AllocateMessageQueueStrategy
	processQueueTable            map[model.MessageQueue]*model.ProcessQueue // both subscribe topic and retry group
	processQueueTableLock        sync.RWMutex
	mutex                        sync.Mutex
	offsetStore                  OffsetStore
	consumerConfig               *rocketmq_api_model.RocketMqConsumerConfig
}

func (r *Rebalance) GetMqTableInfo() map[model.MessageQueue]model.ProcessQueueInfo {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	mqTable := map[model.MessageQueue]model.ProcessQueueInfo{}
	for messageQueue, processQueue := range r.processQueueTable {
		mqTable[messageQueue] = processQueue.ChangeToProcessQueueInfo()
	}
	return mqTable
}

func (r *Rebalance) GetProcessQueue(messageQueue model.MessageQueue) *model.ProcessQueue {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	return r.processQueueTable[messageQueue]
}

func (r *Rebalance) ClearProcessQueue(offsetTable map[model.MessageQueue]int64) {
	defer r.processQueueTableLock.Unlock()
	r.processQueueTableLock.Lock()
	for mq, _ := range offsetTable {
		processQueue, ok := r.processQueueTable[mq]
		if !ok {
			continue
		}
		processQueue.Clear()
	}

}

func (r *Rebalance) GetProcessQueueList() (messageQueueList []model.MessageQueue, processQueueList []*model.ProcessQueue) {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	for messageQueue, processQueue := range r.processQueueTable {
		processQueueList = append(processQueueList, processQueue)
		messageQueueList = append(messageQueueList, messageQueue)
	}
	return
}

//removeUnnecessaryMessageQueue you should drop it first
func (r *Rebalance) RemoveProcessQueue(messageQueue *model.MessageQueue) {
	r.offsetStore.Persist(messageQueue)
	r.offsetStore.RemoveOffset(messageQueue)
	r.removeMessageQueueFromMap(*messageQueue)
}
func (r *Rebalance) removeMessageQueueFromMap(messageQueue model.MessageQueue) {
	defer r.processQueueTableLock.Unlock()
	r.processQueueTableLock.Lock()
	delete(r.processQueueTable, messageQueue)

}

func NewRebalance(groupName string, subscription map[string]string, mqClient RocketMqClient, offsetStore OffsetStore, consumerConfig *rocketmq_api_model.RocketMqConsumerConfig) *Rebalance {
	subscriptionInner := make(map[string]*model.SubscriptionData)
	for topic, subExpression := range subscription {
		subData := &model.SubscriptionData{
			Topic:      topic,
			SubString:  subExpression,
			SubVersion: time.Now().Unix(),
		}
		subscriptionInner[topic] = subData
	}
	// put retry
	retryTopic := constant.RETRY_GROUP_TOPIC_PREFIX + groupName
	subscriptionInner[retryTopic] = &model.SubscriptionData{
		Topic:      retryTopic,
		SubString:  "*",
		SubVersion: time.Now().Unix(),
	}
	return &Rebalance{
		groupName:                    groupName,
		mqClient:                     mqClient,
		offsetStore:                  offsetStore,
		SubscriptionInner:            subscriptionInner,
		allocateMessageQueueStrategy: service_allocate_message.GetAllocateMessageQueueStrategyByConfig("default"),
		messageModel:                 "CLUSTERING",
		processQueueTable:            make(map[model.MessageQueue]*model.ProcessQueue),
		consumerConfig:               consumerConfig,
	}
}

func (r *Rebalance) DoRebalance() {
	r.mutex.Lock()
	defer r.mutex.Unlock()
	for topic, _ := range r.SubscriptionInner {
		r.rebalanceByTopic(topic)
	}
}

type ConsumerIdSorter []string

func (self ConsumerIdSorter) Len() int {
	return len(self)
}
func (self ConsumerIdSorter) Swap(i, j int) {
	self[i], self[j] = self[j], self[i]
}
func (self ConsumerIdSorter) Less(i, j int) bool {
	if self[i] < self[j] {
		return true
	}
	return false
}

func (r *Rebalance) rebalanceByTopic(topic string) error {
	var cidAll []string
	cidAll, err := r.findConsumerIdList(topic, r.groupName)
	if err != nil {
		glog.Error(err)
		return err
	}
	r.topicSubscribeInfoTableLock.RLock()
	mqs := r.mqClient.GetTopicSubscribeInfo(topic)
	r.topicSubscribeInfoTableLock.RUnlock()
	if len(mqs) > 0 && len(cidAll) > 0 {
		var messageQueues model.MessageQueues = mqs
		var consumerIdSorter ConsumerIdSorter = cidAll

		sort.Sort(messageQueues)
		sort.Sort(consumerIdSorter)
	}
	allocateResult, err := r.allocateMessageQueueStrategy.Allocate(r.groupName, r.mqClient.GetClientId(), mqs, cidAll)

	if err != nil {
		glog.Error(err)
		return err
	}

	glog.V(2).Infof("rebalance topic[%s]", topic)
	r.updateProcessQueueTableInRebalance(topic, allocateResult)
	return nil
}

func (r *Rebalance) updateProcessQueueTableInRebalance(topic string, mqSet []model.MessageQueue) {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	r.removeTheQueueDontBelongHere(topic, mqSet)
	r.putTheQueueToProcessQueueTable(topic, mqSet)

}
func (r *Rebalance) removeTheQueueDontBelongHere(topic string, mqSet []model.MessageQueue) {
	// there is n^2 todo improve
	for key, value := range r.processQueueTable {
		if topic != key.Topic {
			continue
		}
		needDelete := true
		for _, messageQueueItem := range mqSet {
			if key == messageQueueItem {
				needDelete = false
				break
			}
		}
		if needDelete {
			value.SetDrop(true)
			delete(r.processQueueTable, key)
		}
	}
}

func (r *Rebalance) putTheQueueToProcessQueueTable(topic string, mqSet []model.MessageQueue) {
	for index, mq := range mqSet {
		_, ok := r.processQueueTable[mq]
		if !ok {
			pullRequest := new(model.PullRequest)
			pullRequest.ConsumerGroup = r.groupName
			pullRequest.MessageQueue = &mqSet[index]
			pullRequest.NextOffset = r.computePullFromWhere(&mq)
			pullRequest.ProcessQueue = model.NewProcessQueue()
			r.processQueueTable[mq] = pullRequest.ProcessQueue
			r.mqClient.EnqueuePullMessageRequest(pullRequest)
		}
	}

}
func (r *Rebalance) computePullFromWhere(mq *model.MessageQueue) int64 {
	var result int64 = -1
	lastOffset := r.offsetStore.ReadOffset(mq, READ_FROM_STORE)
	switch r.consumerConfig.ConsumeFromWhere {
	case rocketmq_api_model.CONSUME_FROM_LAST_OFFSET:
		if lastOffset >= 0 {
			result = lastOffset
		} else {
			if strings.HasPrefix(mq.Topic, constant.RETRY_GROUP_TOPIC_PREFIX) {
				result = 0
			} else {
				result = r.mqClient.GetMaxOffset(mq)
			}
		}
		break
	case rocketmq_api_model.CONSUME_FROM_FIRST_OFFSET:
		if lastOffset >= 0 {
			result = lastOffset
		} else {
			result = 0 // use the begin offset
		}
		break
	case rocketmq_api_model.CONSUME_FROM_TIMESTAMP:
		if lastOffset >= 0 {
			result = lastOffset
		} else {
			if strings.HasPrefix(mq.Topic, constant.RETRY_GROUP_TOPIC_PREFIX) {
				result = 0
			} else {
				result = r.mqClient.SearchOffset(mq, r.consumerConfig.ConsumeTimestamp)
			}
		}
		break
	default:

	}

	return result
}

func (r *Rebalance) findConsumerIdList(topic string, groupName string) ([]string, error) {
	brokerAddr, ok := r.mqClient.FindBrokerAddrByTopic(topic)
	if !ok {
		err := r.mqClient.UpdateTopicRouteInfoFromNameServer(topic)
		if err != nil {
			glog.Error(err)
		}
		brokerAddr, ok = r.mqClient.FindBrokerAddrByTopic(topic)
	}

	if ok {
		return r.getConsumerIdListByGroup(brokerAddr, groupName, 3000)
	}

	return nil, errors.New("can't find broker")

}

func (r *Rebalance) getConsumerIdListByGroup(addr string, consumerGroup string, timeoutMillis int64) ([]string, error) {
	requestHeader := new(header.GetConsumerListByGroupRequestHeader)
	requestHeader.ConsumerGroup = consumerGroup

	request := remoting.NewRemotingCommand(remoting.GET_CONSUMER_LIST_BY_GROUP, requestHeader)

	response, err := r.mqClient.GetRemotingClient().InvokeSync(addr, request, timeoutMillis)
	if err != nil {
		glog.Error(err)
		return nil, err
	}
	if response.Code == remoting.SUCCESS {
		getConsumerListByGroupResponseBody := new(header.GetConsumerListByGroupResponseBody)
		bodyjson := strings.Replace(string(response.Body), "0:", "\"0\":", -1)
		bodyjson = strings.Replace(bodyjson, "1:", "\"1\":", -1)
		err := json.Unmarshal([]byte(bodyjson), getConsumerListByGroupResponseBody)
		if err != nil {
			glog.Error(err)
			return nil, err
		}
		return getConsumerListByGroupResponseBody.ConsumerIdList, nil
	}
	return nil, errors.New("getConsumerIdListByGroup error=" + response.Remark)
}
