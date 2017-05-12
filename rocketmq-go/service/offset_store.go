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
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
	"strconv"
	"sync"
)

const (
	MEMORY_FIRST_THEN_STORE = 0
	READ_FROM_MEMORY        = 1
	READ_FROM_STORE         = 2
)

type OffsetStore interface {
	UpdateOffset(mq *model.MessageQueue, offset int64, increaseOnly bool)
	ReadOffset(mq *model.MessageQueue, readType int) int64
	Persist(mq *model.MessageQueue)
	RemoveOffset(mq *model.MessageQueue)
}
type RemoteOffsetStore struct {
	groupName       string
	mqClient        RocketMqClient
	offsetTable     map[model.MessageQueue]int64
	offsetTableLock sync.RWMutex
}

func RemoteOffsetStoreInit(groupName string, mqClient RocketMqClient) OffsetStore {
	offsetStore := new(RemoteOffsetStore)
	offsetStore.groupName = groupName
	offsetStore.mqClient = mqClient
	offsetStore.offsetTable = make(map[model.MessageQueue]int64)
	return offsetStore
}
func (self *RemoteOffsetStore) RemoveOffset(mq *model.MessageQueue) {
	defer self.offsetTableLock.Unlock()
	self.offsetTableLock.Lock()
	delete(self.offsetTable, *mq)
}

func (self *RemoteOffsetStore) Persist(mq *model.MessageQueue) {
	brokerAddr := self.mqClient.FetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		self.mqClient.TryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = self.mqClient.FetchMasterBrokerAddress(mq.BrokerName)
	}
	self.offsetTableLock.RLock()
	offset := self.offsetTable[*mq]
	self.offsetTableLock.RUnlock()
	updateConsumerOffsetRequestHeader := &header.UpdateConsumerOffsetRequestHeader{ConsumerGroup: self.groupName, Topic: mq.Topic, QueueId: mq.QueueId, CommitOffset: offset}
	requestCommand := remoting.NewRemotingCommand(remoting.UPDATE_CONSUMER_OFFSET, updateConsumerOffsetRequestHeader)
	self.mqClient.GetRemotingClient().InvokeOneWay(brokerAddr, requestCommand, 1000*5)
}

func (self *RemoteOffsetStore) ReadOffset(mq *model.MessageQueue, readType int) int64 {

	switch readType {
	case MEMORY_FIRST_THEN_STORE:
	case READ_FROM_MEMORY:
		self.offsetTableLock.RLock()
		offset, ok := self.offsetTable[*mq]
		self.offsetTableLock.RUnlock()
		if ok {
			return offset
		} else {
			return -1
		}
	case READ_FROM_STORE:
		offset, err := self.fetchConsumeOffsetFromBroker(mq)
		if err != nil {
			glog.Error(err)
			return -1
		}
		glog.V(2).Info("READ_FROM_STORE", offset)
		self.UpdateOffset(mq, offset, false)
		return offset
	}

	return -1

}

func (self *RemoteOffsetStore) fetchConsumeOffsetFromBroker(mq *model.MessageQueue) (int64, error) {
	brokerAddr, _, found := self.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName, 0, false)

	if !found {
		brokerAddr, _, found = self.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName, 0, false)
	}

	if found {
		requestHeader := &header.QueryConsumerOffsetRequestHeader{}
		requestHeader.Topic = mq.Topic
		requestHeader.QueueId = mq.QueueId
		requestHeader.ConsumerGroup = self.groupName
		return self.queryConsumerOffset(brokerAddr, requestHeader, 3000)
	}

	return -1, errors.New("fetch consumer offset error")
}

func (self RemoteOffsetStore) queryConsumerOffset(addr string, requestHeader *header.QueryConsumerOffsetRequestHeader, timeoutMillis int64) (int64, error) {
	remotingCommand := remoting.NewRemotingCommand(remoting.QUERY_CONSUMER_OFFSET, requestHeader)
	reponse, err := self.mqClient.GetRemotingClient().InvokeSync(addr, remotingCommand, timeoutMillis)
	if err != nil {
		glog.Error(err)
		return -1, err
	}

	if reponse.Code == remoting.QUERY_NOT_FOUND {
		return -1, nil
	}

	if offsetInter, ok := reponse.ExtFields["offset"]; ok {
		if offsetStr, ok := offsetInter.(string); ok {
			offset, err := strconv.ParseInt(offsetStr, 10, 64)
			if err != nil {
				glog.Error(err)
				return -1, err
			}
			return offset, nil

		}
	}
	glog.Error(requestHeader, reponse)
	return -1, errors.New("query offset error")
}

func (self *RemoteOffsetStore) UpdateOffset(mq *model.MessageQueue, offset int64, increaseOnly bool) {
	defer self.offsetTableLock.Unlock()
	self.offsetTableLock.Lock()
	if mq != nil {
		if increaseOnly {
			offsetOld := self.offsetTable[*mq]
			if offsetOld >= offset {
				return
			}
			self.offsetTable[*mq] = offset
		} else {
			self.offsetTable[*mq] = offset
		}

	}
}
