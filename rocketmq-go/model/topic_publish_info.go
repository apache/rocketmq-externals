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
package model

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"sync/atomic"
)

type TopicPublishInfo struct {
	OrderTopic             bool
	HaveTopicRouterInfo    bool
	MessageQueueList       []MessageQueue
	TopicRouteDataInstance *TopicRouteData
	topicQueueIndex        int32
}

func (t *TopicPublishInfo) JudgeTopicPublishInfoOk() (bIsTopicOk bool) {
	bIsTopicOk = (len(t.MessageQueueList) > 0)
	return
}
func (t *TopicPublishInfo) FetchQueueIndex() (index int) {
	qLen := len(t.MessageQueueList)
	if qLen > 0 {
		qIndex := atomic.AddInt32(&t.topicQueueIndex, 1)
		qIndex = qIndex % int32(qLen)
		index = int(qIndex)
	}
	return
}
func BuildTopicSubscribeInfoFromRoteData(topic string, topicRouteData *TopicRouteData) (mqList []*MessageQueue) {
	mqList = make([]*MessageQueue, 0)
	for _, queueData := range topicRouteData.QueueDatas {
		if !constant.ReadAble(queueData.Perm) {
			continue
		}
		var i int32
		for i = 0; i < queueData.ReadQueueNums; i++ {
			mq := &MessageQueue{
				Topic:      topic,
				BrokerName: queueData.BrokerName,
				QueueId:    i,
			}
			mqList = append(mqList, mq)
		}
	}
	return
}

func BuildTopicPublishInfoFromTopicRoteData(topic string, topicRouteData *TopicRouteData) (topicPublishInfo *TopicPublishInfo) {
	topicPublishInfo = &TopicPublishInfo{
		TopicRouteDataInstance: topicRouteData,
		OrderTopic:             false,
		MessageQueueList:       []MessageQueue{}}
	for _, queueData := range topicRouteData.QueueDatas {
		if !constant.WriteAble(queueData.Perm) {
			continue
		}
		for _, brokerData := range topicRouteData.BrokerDatas {
			if brokerData.BrokerName == queueData.BrokerName {
				if len(brokerData.BrokerAddrs["0"]) == 0 {
					break
				}
				var i int32
				for i = 0; i < queueData.WriteQueueNums; i++ {
					messageQueue := MessageQueue{Topic: topic, BrokerName: queueData.BrokerName, QueueId: i}
					topicPublishInfo.MessageQueueList = append(topicPublishInfo.MessageQueueList, messageQueue)
					topicPublishInfo.HaveTopicRouterInfo = true
				}
				break
			}
		}
	}
	return
}
