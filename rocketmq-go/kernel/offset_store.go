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
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
	"strconv"
	"sync"
)

const (
	//MEMORY_FIRST_THEN_STORE memory first then store
	MEMORY_FIRST_THEN_STORE = 0
	//READ_FROM_MEMORY   READ_FROM_MEMORY
	READ_FROM_MEMORY = 1
	//READ_FROM_STORE READ_FROM_STORE
	READ_FROM_STORE = 2
)

//OffsetStore OffsetStore
type OffsetStore interface {
	//update local offsetTable's offset
	updateOffset(mq *model.MessageQueue, offset int64, increaseOnly bool)
	//read offset,from memory or broker
	readOffset(mq *model.MessageQueue, readType int) int64
	//update broker's offset
	persist(mq *model.MessageQueue)
	//remove local offsetTable's offset
	removeOffset(mq *model.MessageQueue)
}

//RemoteOffsetStore offset store on remote
type RemoteOffsetStore struct {
	groupName       string
	mqClient        RocketMqClient
	offsetTable     map[model.MessageQueue]int64
	offsetTableLock *sync.RWMutex
}

func remoteOffsetStoreInit(groupName string, mqClient RocketMqClient) OffsetStore {
	offsetStore := new(RemoteOffsetStore)
	offsetStore.groupName = groupName
	offsetStore.mqClient = mqClient
	offsetStore.offsetTable = make(map[model.MessageQueue]int64)
	offsetStore.offsetTableLock = new(sync.RWMutex)
	return offsetStore
}
func (r *RemoteOffsetStore) removeOffset(mq *model.MessageQueue) {
	defer r.offsetTableLock.Unlock()
	r.offsetTableLock.Lock()
	delete(r.offsetTable, *mq)
}

func (r *RemoteOffsetStore) persist(mq *model.MessageQueue) {
	brokerAddr := r.mqClient.FetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		r.mqClient.TryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = r.mqClient.FetchMasterBrokerAddress(mq.BrokerName)
	}
	r.offsetTableLock.RLock()
	offset := r.offsetTable[*mq]
	r.offsetTableLock.RUnlock()
	updateConsumerOffsetRequestHeader := &header.UpdateConsumerOffsetRequestHeader{ConsumerGroup: r.groupName, Topic: mq.Topic, QueueId: mq.QueueId, CommitOffset: offset}
	requestCommand := remoting.NewRemotingCommand(remoting.UPDATE_CONSUMER_OFFSET, updateConsumerOffsetRequestHeader)
	r.mqClient.GetRemotingClient().InvokeOneWay(brokerAddr, requestCommand, 1000*5)
}

func (r *RemoteOffsetStore) readOffset(mq *model.MessageQueue, readType int) int64 {

	switch readType {
	case MEMORY_FIRST_THEN_STORE:
	case READ_FROM_MEMORY:
		r.offsetTableLock.RLock()
		offset, ok := r.offsetTable[*mq]
		r.offsetTableLock.RUnlock()
		if ok {
			return offset
		}
		return -1

	case READ_FROM_STORE:
		offset, err := r.fetchConsumeOffsetFromBroker(mq)
		if err != nil {
			glog.Error(err)
			return -1
		}
		glog.V(2).Info("READ_FROM_STORE", offset)
		r.updateOffset(mq, offset, false)
		return offset
	}

	return -1

}

func (r *RemoteOffsetStore) fetchConsumeOffsetFromBroker(mq *model.MessageQueue) (int64, error) {
	brokerAddr, _, found := r.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName, 0, false)

	if !found {
		brokerAddr, _, found = r.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName, 0, false)
	}

	if found {
		requestHeader := &header.QueryConsumerOffsetRequestHeader{}
		requestHeader.Topic = mq.Topic
		requestHeader.QueueId = mq.QueueId
		requestHeader.ConsumerGroup = r.groupName
		return r.queryConsumerOffset(brokerAddr, requestHeader, 3000)
	}

	return -1, errors.New("fetch consumer offset error")
}

func (r RemoteOffsetStore) queryConsumerOffset(addr string, requestHeader *header.QueryConsumerOffsetRequestHeader, timeoutMillis int64) (int64, error) {
	remotingCommand := remoting.NewRemotingCommand(remoting.QUERY_CONSUMER_OFFSET, requestHeader)
	response, err := r.mqClient.GetRemotingClient().InvokeSync(addr, remotingCommand, timeoutMillis)
	if err != nil {
		glog.Error(err)
		return -1, err
	}

	if response.Code == remoting.QUERY_NOT_FOUND {
		return -1, nil
	}

	if offsetInter, ok := response.ExtFields["offset"]; ok {
		if offsetStr, ok := offsetInter.(string); ok {
			offset, err := strconv.ParseInt(offsetStr, 10, 64)
			if err != nil {
				glog.Error(err)
				return -1, err
			}
			return offset, nil

		}
	}
	glog.Error(requestHeader, response)
	return -1, errors.New("query offset error")
}

func (r *RemoteOffsetStore) updateOffset(mq *model.MessageQueue, offset int64, increaseOnly bool) {
	defer r.offsetTableLock.Unlock()
	r.offsetTableLock.Lock()
	if mq != nil {
		if increaseOnly {
			offsetOld := r.offsetTable[*mq]
			if offsetOld >= offset {
				return
			}
			r.offsetTable[*mq] = offset
		} else {
			r.offsetTable[*mq] = offset
		}

	}
}
