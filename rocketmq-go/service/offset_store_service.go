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
	//"github.com/golang/glog"
	"fmt"
	"sync"
	"sync/atomic"
)

const (
	MemoryFirstThenStore = 0
	ReadFromMemory       = 1
	ReadFromStore        = 2
)

type OffsetStore interface {
	//load() error
	updateOffset(mq *MessageQueue, offset int64, increaseOnly bool)
	readOffset(mq *MessageQueue, flag int) int64
	//persistAll(mqs []MessageQueue)
	//persist(mq MessageQueue)
	//removeOffset(mq MessageQueue)
	//cloneOffsetTable(topic string) map[MessageQueue]int64
}
type RemoteOffsetStore struct {
	groupName       string
	mqClient        *MqClient
	offsetTable     map[MessageQueue]int64
	offsetTableLock sync.RWMutex
}

func (r *RemoteOffsetStore) readOffset(mq *MessageQueue, readType int) int64 {

	switch readType {
	case MemoryFirstThenStore:
	case ReadFromMemory:
		r.offsetTableLock.RLock()
		offset, ok := r.offsetTable[*mq]
		r.offsetTableLock.RUnlock()
		if ok {
			return offset
		} else if readType == ReadFromMemory {
			return -1
		}
	case ReadFromStore:
		offset, err := r.fetchConsumeOffsetFromBroker(mq)

		if err != nil {
			fmt.Println(err)
			return -1
		}
		r.updateOffset(mq, offset, false)
		return offset
	}

	return -1

}

func (r *RemoteOffsetStore) fetchConsumeOffsetFromBroker(mq *MessageQueue) (int64, error) {
	brokerAddr, _, found := r.mqClient.findBrokerAddressInSubscribe(mq.brokerName, 0, false)

	if !found {
		if _, err := r.mqClient.updateTopicRouteInfoFromNameServerKernel(mq.topic, false, DefaultProducer{}); err != nil {
			return 0, err
		}
		brokerAddr, _, found = r.mqClient.findBrokerAddressInSubscribe(mq.brokerName, 0, false)
	}

	if found {
		requestHeader := &QueryConsumerOffsetRequestHeader{}
		requestHeader.Topic = mq.topic
		requestHeader.QueueId = mq.queueId
		requestHeader.ConsumerGroup = r.groupName
		return r.mqClient.queryConsumerOffset(brokerAddr, requestHeader, 3000)
	}

	return 0, errors.New("fetch consumer offset error")
}

func (r *RemoteOffsetStore) persist(mq *MessageQueue) {
	offset, ok := r.offsetTable[*mq]
	if ok {
		err := r.updateConsumeOffsetToBroker(mq, offset)
		if err != nil {
			fmt.Println(err)
		}
	}
}

type UpdateConsumerOffsetRequestHeader struct {
	consumerGroup string
	topic         string
	queueId       int32
	commitOffset  int64
}

func (r *RemoteOffsetStore) updateConsumeOffsetToBroker(mq *MessageQueue, offset int64) error {
	addr, found, _ := r.mqClient.findBrokerAddressInSubscribe(mq.brokerName, 0, false)
	if !found {
		if _, err := r.mqClient.updateTopicRouteInfoFromNameServerKernel(mq.topic, false, DefaultProducer{}); err != nil {
			return err
		}
		addr, found, _ = r.mqClient.findBrokerAddressInSubscribe(mq.brokerName, 0, false)
	}

	if found {
		requestHeader := &UpdateConsumerOffsetRequestHeader{
			consumerGroup: r.groupName,
			topic:         mq.topic,
			queueId:       mq.queueId,
			commitOffset:  offset,
		}

		r.mqClient.updateConsumerOffsetOneway(addr, requestHeader, 5*1000)
		return nil
	}
	return errors.New("not found broker")
}

func (r *RemoteOffsetStore) updateOffset(mq *MessageQueue, offset int64, increaseOnly bool) {
	if mq != nil {
		r.offsetTableLock.RLock()
		offsetOld, ok := r.offsetTable[*mq]
		r.offsetTableLock.RUnlock()
		if !ok {
			r.offsetTableLock.Lock()
			r.offsetTable[*mq] = offset
			r.offsetTableLock.Unlock()
		} else {
			if increaseOnly {
				atomic.AddInt64(&offsetOld, offset)
				r.offsetTableLock.Lock()
				r.offsetTable[*mq] = offsetOld
				r.offsetTableLock.Unlock()
			} else {
				r.offsetTableLock.Lock()
				r.offsetTable[*mq] = offset
				r.offsetTableLock.Unlock()
			}
		}

	}

}