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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/allocate"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
	"sort"
	"strings"
	"sync"
	"time"
)

type rebalance struct {
	groupName                    string
	messageModel                 string
	topicSubscribeInfoTableLock  sync.RWMutex
	subscriptionInner            map[string]*model.SubscriptionData
	subscriptionInnerLock        sync.RWMutex
	mqClient                     RocketMqClient
	allocateMessageQueueStrategy allocate.AllocateMessageQueueStrategy
	processQueueTable            map[model.MessageQueue]*model.ProcessQueue // both subscribe topic and retry group
	processQueueTableLock        sync.RWMutex
	mutex                        sync.Mutex
	offsetStore                  OffsetStore
	consumerConfig               *rocketmqm.MqConsumerConfig
}

//when invoke GET_CONSUMER_RUNNING_INFO, getMqTableInfo will return ProcessQueueInfo
func (r *rebalance) getMqTableInfo() map[model.MessageQueue]model.ProcessQueueInfo {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	mqTable := map[model.MessageQueue]model.ProcessQueueInfo{}
	for messageQueue, processQueue := range r.processQueueTable {
		mqTable[messageQueue] = processQueue.ChangeToProcessQueueInfo()
	}
	return mqTable
}

func (r *rebalance) getProcessQueue(messageQueue model.MessageQueue) *model.ProcessQueue {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	return r.processQueueTable[messageQueue]
}

func (r *rebalance) clearProcessQueue(offsetTable map[model.MessageQueue]int64) {
	defer r.processQueueTableLock.Unlock()
	r.processQueueTableLock.Lock()
	for mq := range offsetTable {
		processQueue, ok := r.processQueueTable[mq]
		if !ok {
			continue
		}
		processQueue.Clear()
	}

}

func (r *rebalance) getProcessQueueList() (messageQueueList []model.MessageQueue, processQueueList []*model.ProcessQueue) {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	for messageQueue, processQueue := range r.processQueueTable {
		processQueueList = append(processQueueList, processQueue)
		messageQueueList = append(messageQueueList, messageQueue)
	}
	return
}

//removeUnnecessaryMessageQueue you should drop it first
func (r *rebalance) removeProcessQueue(messageQueue *model.MessageQueue) {
	r.offsetStore.Persist(messageQueue)
	r.offsetStore.RemoveOffset(messageQueue)
	r.removeMessageQueueFromMap(*messageQueue)
}
func (r *rebalance) removeMessageQueueFromMap(messageQueue model.MessageQueue) {
	defer r.processQueueTableLock.Unlock()
	r.processQueueTableLock.Lock()
	delete(r.processQueueTable, messageQueue)

}

func newRebalance(groupName string, subscription map[string]string, mqClient RocketMqClient, offsetStore OffsetStore, consumerConfig *rocketmqm.MqConsumerConfig) *rebalance {
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
	return &rebalance{
		groupName:                    groupName,
		mqClient:                     mqClient,
		offsetStore:                  offsetStore,
		subscriptionInner:            subscriptionInner,
		allocateMessageQueueStrategy: allocate.GetAllocateMessageQueueStrategyByConfig("default"),
		messageModel:                 "CLUSTERING",
		processQueueTable:            make(map[model.MessageQueue]*model.ProcessQueue),
		consumerConfig:               consumerConfig,
	}
}

func (r *rebalance) doRebalance() {
	r.mutex.Lock()
	defer r.mutex.Unlock()
	for topic := range r.subscriptionInner {
		r.rebalanceByTopic(topic)
	}
}

type consumerIdSorter []string

func (c consumerIdSorter) Len() int {
	return len(c)
}
func (c consumerIdSorter) Swap(i, j int) {
	c[i], c[j] = c[j], c[i]
}
func (c consumerIdSorter) Less(i, j int) bool {
	if c[i] < c[j] {
		return true
	}
	return false
}

func (r *rebalance) rebalanceByTopic(topic string) error {
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
		var consumerIdSorter consumerIdSorter = cidAll

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

func (r *rebalance) updateProcessQueueTableInRebalance(topic string, mqSet []model.MessageQueue) {
	defer r.processQueueTableLock.RUnlock()
	r.processQueueTableLock.RLock()
	r.removeTheQueueDontBelongHere(topic, mqSet)
	r.putTheQueueToProcessQueueTable(topic, mqSet)

}
func (r *rebalance) removeTheQueueDontBelongHere(topic string, mqSet []model.MessageQueue) {
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

func (r *rebalance) putTheQueueToProcessQueueTable(topic string, mqSet []model.MessageQueue) {
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
func (r *rebalance) computePullFromWhere(mq *model.MessageQueue) int64 {
	var result int64 = -1
	lastOffset := r.offsetStore.ReadOffset(mq, READ_FROM_STORE)
	switch r.consumerConfig.ConsumeFromWhere {
	case rocketmqm.CONSUME_FROM_LAST_OFFSET:
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
	case rocketmqm.CONSUME_FROM_FIRST_OFFSET:
		if lastOffset >= 0 {
			result = lastOffset
		} else {
			result = 0 // use the begin offset
		}
		break
	case rocketmqm.CONSUME_FROM_TIMESTAMP:
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

func (r *rebalance) findConsumerIdList(topic string, groupName string) ([]string, error) {
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

func (r *rebalance) getConsumerIdListByGroup(addr string, consumerGroup string, timeoutMillis int64) ([]string, error) {
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
		glog.Info("string(response.Body)" + string(response.Body) + "todo todo") // todo check
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
