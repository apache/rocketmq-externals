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
package consumer

import (
	"bytes"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"math/rand"
	"time"
	"github.com/golang/glog"
)

type pullAPIWrapper struct {
	mqClient               *service.MQClient
	api                    *service.MQClientAPI
	consumerGroup          string
	unitMode               bool
	pullFromWhichNodeTable map[*message.MessageQueue]int64
	connectBrokerByUser    bool
	defaultBrokerID        int64
	random                 *rand.Rand
	filterMessageHookList  []model.FilterMessageHook
}

func buildPullAPIWrapper(client *service.MQClient, group string, unitMode bool) pullAPIWrapper {
	return pullAPIWrapper{
		mqClient:               client,
		consumerGroup:          group,
		unitMode:               unitMode,
		pullFromWhichNodeTable: make(map[*message.MessageQueue]int64, 32),
		connectBrokerByUser:    false,
		defaultBrokerID:        1, // TODO
		random:                 rand.New(rand.NewSource(time.Now().Unix())),
	}
}

func (p *pullAPIWrapper) processPullResult(mq *message.MessageQueue, pr model.PullResult,
	subData model.SubscriptionData) model.PullResult {
	p.updatePullFromWhichNode(mq, pr.SuggestWhichBrokerID())

	if pr.PullStatus() == model.Found {
		buffer := bytes.NewBuffer(pr.MessageBinary())
		msgList := message.Decodes(buffer)

		var msgListFilterAgain []*message.MessageExt = msgList
		//copy(msgListFilter, msgList)  TODO consider: need hard copy?
		if len(subData.TagsSet) > 0 && !subData.ClassFilterMode {
			msgListFilterAgain = make([]*message.MessageExt, len(msgList))
			for _, msg := range msgList {
				if msg.Tags() != "" {
					_, found := subData.TagsSet[msg.Tags()]
					if found {
						msgListFilterAgain = append(msgListFilterAgain, msg) // TODO optimize
					}
				}
			}
		}

		if p.hasHook() {
			// TODO
		}

		for _, v := range msgListFilterAgain { // TODO can optimize?
			message.PutProperty(&(v.Message), message.MessageConst.PropertyMinOffset, fmt.Sprint(pr.MinOffset()))
			message.PutProperty(&(v.Message), message.MessageConst.PropertyMaxOffset, fmt.Sprint(pr.MaxOffset()))
		}
		pr.SetMsgFoundList(msgListFilterAgain)
	}

	pr.SetMessageBinary(nil)
	return pr
}

func (p *pullAPIWrapper) updatePullFromWhichNode(mq *message.MessageQueue, brokerID int64) {
	p.pullFromWhichNodeTable[mq] = brokerID
}

func (p *pullAPIWrapper) hasHook() bool {
	return len(p.filterMessageHookList) > 0
}

func (p *pullAPIWrapper) executeHook(ctx model.FilterMessageContext) {
	for _, v := range p.filterMessageHookList {
		// TODO v.filterMessage(ctx)
		v.Name()
	}
}

func (p *pullAPIWrapper) pullKernel(mq *message.MessageQueue, subExp string, subVersion, offset, commitOffset int64,
	maxNum, sysFlag int, brokerSuspendMaxTime, timeout time.Duration, mode remoting.CommunicationMode,
	callback model.PullCallback) (model.PullResult, error) {
	findBrokerResult := p.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName(),
		p.recalculatePullFromWhichNode(mq), false)
	if findBrokerResult == nil {
		p.mqClient.UpdateTopicRouteInfoFromNameServer(mq.Topic(), false)
		findBrokerResult = p.mqClient.FindBrokerAddressInSubscribe(mq.BrokerName(),
			p.recalculatePullFromWhichNode(mq), false)
	}
	sysFlagInner := sysFlag
	if findBrokerResult.Slave {
		sysFlagInner = ClearCommitOffsetFlag(sysFlagInner)
	}

	requestHeader := header.PullMessageRequestHeader{
		ConsumerGroup:  p.consumerGroup,
		Topic:          mq.Topic(),
		QueueID:        mq.QueueID(),
		QueueOffset:    offset,
		MaxMsgNums:     maxNum,
		SysFlag:        sysFlagInner,
		CommitOffset:   commitOffset,
		SuspendTimeout: brokerSuspendMaxTime,
		Subscription:   subExp,
		SubVersion:     subVersion,
	}

	brokerAddress := findBrokerResult.BrokerAddress
	if HasClassFilterFlag(sysFlagInner) {
		brokerAddress = p.computePullFromWhichFilterServer(mq.Topic(), brokerAddress)
	}

	pullResult, err := p.api.PullMessage(brokerAddress, requestHeader, timeout, mode, callback)
	if err != nil {
		return nil, model.NewMQBrokerError(0, fmt.Sprintf("The broker [%s] not exist", mq.BrokerName()))
	}
	return pullResult, nil
}

func (p *pullAPIWrapper) recalculatePullFromWhichNode(mq *message.MessageQueue) int64 {
	if p.connectBrokerByUser {
		return p.defaultBrokerID
	}
	suggest := p.pullFromWhichNodeTable[mq]

	if suggest != 0 {
		return suggest
	}

	return 0 //        return MixAll.MASTER_ID
}

func (p *pullAPIWrapper) computePullFromWhichFilterServer(topic, brokerAddress string) (address string) {
	defer func() {
		if address == "" {
			glog.Fatalf("Find Filter Server Failed, Broker Addrress: %s, topic: ", brokerAddress, topic)
		}
	}()
	
	topicRouteTable := p.mqClient.TopicRouteTable()
	if topicRouteTable == nil {
		return
	}

	topicRouteData, found := topicRouteTable[topic]
	if !found {
		return
	}

	list := topicRouteData.FilterServerTable()[brokerAddress]
	 if len(list) > 0 {
		 address = list[p.randomNum() % len(list)]
	 }
	return
}

func (p *pullAPIWrapper) randomNum() int {
	return int(p.random.Uint32())
}

func (p *pullAPIWrapper) registerFilterMessageHook(hooks []model.FilterMessageHook) {
	p.filterMessageHookList = hooks
}

func (p *pullAPIWrapper) setDefaultBrokerId(brokerID int64) {
	p.defaultBrokerID = brokerID
}
