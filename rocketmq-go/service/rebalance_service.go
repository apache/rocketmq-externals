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
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
)

import (
	"errors"
	//"github.com/golang/glog"
	"fmt"
	"sort"
	"sync"
)

type SubscriptionData struct {
	Topic           string
	SubString       string
	ClassFilterMode bool
	TagsSet         []string
	CodeSet         []string
	SubVersion      int64
}

type Rebalance struct {
	groupName                    string
	messageModel                 string
	topicSubscribeInfoTable      map[string][]*MessageQueue
	topicSubscribeInfoTableLock  sync.RWMutex
	subscriptionInner            map[string]*SubscriptionData
	subscriptionInnerLock        sync.RWMutex
	mqClient                     *MqClient
	allocateMessageQueueStrategy AllocateMessageQueueStrategy
	consumer                     *DefaultConsumer
	producer                     *DefaultProducer
	processQueueTable            map[MessageQueue]int32
	processQueueTableLock        sync.RWMutex
	mutex                        sync.Mutex
}

func NewRebalance() *Rebalance {
	return &Rebalance{
		topicSubscribeInfoTable:      make(map[string][]*MessageQueue),
		subscriptionInner:            make(map[string]*SubscriptionData),
		allocateMessageQueueStrategy: new(AllocateMessageQueueAveragely),
		messageModel:                 "CLUSTERING",
		processQueueTable:            make(map[MessageQueue]int32),
	}
}

func (r *Rebalance) doRebalance() {
	r.mutex.Lock()
	defer r.mutex.Unlock()
	for topic, _ := range r.subscriptionInner {
		r.rebalanceByTopic(topic)
	}
}

type ConsumerIdSorter []string

func (r ConsumerIdSorter) Len() int      { return len(r) }
func (r ConsumerIdSorter) Swap(i, j int) { r[i], r[j] = r[j], r[i] }
func (r ConsumerIdSorter) Less(i, j int) bool {
	if r[i] < r[j] {
		return true
	}
	return false
}

type AllocateMessageQueueStrategy interface {
	allocate(consumerGroup string, currentCID string, mqAll []*MessageQueue, cidAll []string) ([]*MessageQueue, error)
}
type AllocateMessageQueueAveragely struct{}

func (r *AllocateMessageQueueAveragely) allocate(consumerGroup string, currentCID string, mqAll []*MessageQueue, cidAll []string) ([]*MessageQueue, error) {
	if currentCID == "" {
		return nil, errors.New("currentCID is empty")
	}

	if mqAll == nil || len(mqAll) == 0 {
		return nil, errors.New("mqAll is nil or mqAll empty")
	}

	if cidAll == nil || len(cidAll) == 0 {
		return nil, errors.New("cidAll is nil or cidAll empty")
	}

	result := make([]*MessageQueue, 0)
	for i, cid := range cidAll {
		if cid == currentCID {
			mqLen := len(mqAll)
			cidLen := len(cidAll)
			mod := mqLen % cidLen
			var averageSize int
			if mqLen < cidLen {
				averageSize = 1
			} else {
				if mod > 0 && i < mod {
					averageSize = mqLen/cidLen + 1
				} else {
					averageSize = mqLen / cidLen
				}
			}

			var startIndex int
			if mod > 0 && i < mod {
				startIndex = i * averageSize
			} else {
				startIndex = i*averageSize + mod
			}

			var min int
			if averageSize > mqLen-startIndex {
				min = mqLen - startIndex
			} else {
				min = averageSize
			}

			for j := 0; j < min; j++ {
				result = append(result, mqAll[(startIndex+j)%mqLen])
			}
			return result, nil

		}
	}

	return nil, errors.New("cant't find currentCID")
}

func (r *Rebalance) rebalanceByTopic(topic string) error {
	cidAll, err := r.mqClient.findConsumerIdList(topic, r.groupName)
	if err != nil {
		fmt.Println(err)
		return err
	}

	r.topicSubscribeInfoTableLock.RLock()
	mqs, ok := r.topicSubscribeInfoTable[topic]
	r.topicSubscribeInfoTableLock.RUnlock()
	if ok && len(mqs) > 0 && len(cidAll) > 0 {
		var messageQueues MessageQueues = mqs
		var consumerIdSorter ConsumerIdSorter = cidAll

		sort.Sort(messageQueues)
		sort.Sort(consumerIdSorter)
	}

	allocateResult, err := r.allocateMessageQueueStrategy.allocate(r.groupName, r.mqClient.clientId, mqs, cidAll)

	if err != nil {
		fmt.Println(err)
		return err
	}

	fmt.Println("rebalance topic[%s]", topic)
	r.updateProcessQueueTableInRebalance(topic, allocateResult)
	return nil
}

func (r *Rebalance) updateProcessQueueTableInRebalance(topic string, mqSet []*MessageQueue) {
	for _, mq := range mqSet {
		r.processQueueTableLock.RLock()
		_, ok := r.processQueueTable[*mq]
		r.processQueueTableLock.RUnlock()
		if !ok {
			pullRequest := new(PullRequest)
			pullRequest.consumerGroup = r.groupName
			pullRequest.messageQueue = mq
			pullRequest.nextOffset = r.computePullFromWhere(mq)
			r.mqClient.pullMessageService.pullRequestQueue <- pullRequest
			r.processQueueTableLock.Lock()
			r.processQueueTable[*mq] = 1
			r.processQueueTableLock.Unlock()
		}
	}

}

func (r *Rebalance) computePullFromWhere(mq *MessageQueue) int64 {
	var result int64 = -1
	lastOffset := r.consumer.offsetStore.readOffset(mq, ReadFromStore)

	if lastOffset >= 0 {
		result = lastOffset
	} else {
		result = 0
	}
	return result
}
