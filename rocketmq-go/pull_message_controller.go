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
package rocketmq

import (
	"bytes"
	"compress/zlib"
	"encoding/binary"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"io/ioutil"
	"strconv"
	"time"
)

type PullMessageController struct {
	mqClient      service.RocketMqClient
	clientFactory *ClientFactory
}

func NewPullMessageController(mqClient service.RocketMqClient, clientFactory *ClientFactory) *PullMessageController {
	return &PullMessageController{
		mqClient:      mqClient,
		clientFactory: clientFactory,
	}
}

func (self *PullMessageController) Start() {
	go func() {
		for {
			pullRequest := self.mqClient.DequeuePullMessageRequest()
			self.pullMessage(pullRequest)
		}
	}()
}

func (self *PullMessageController) needDelayPullMessage(mqPushConsumer *DefaultMQPushConsumer, pullRequest *model.PullRequest) (needDelayTime int64) {
	if pullRequest.ProcessQueue.GetMsgCount() > mqPushConsumer.ConsumerConfig.PullThresholdForQueue {
		return mqPushConsumer.ConsumerConfig.PullTimeDelayMillsWhenFlowControl
	}
	if pullRequest.ProcessQueue.GetMaxSpan() > mqPushConsumer.ConsumerConfig.ConsumeConcurrentlyMaxSpan {
		return mqPushConsumer.ConsumerConfig.PullTimeDelayMillsWhenFlowControl
	}
	return
}

func (self *PullMessageController) pullMessageLater(pullRequest *model.PullRequest, millisecond int64) {
	go func() {
		timeoutTimer := time.NewTimer(time.Duration(millisecond) * time.Millisecond)
		<-timeoutTimer.C
		self.pullMessage(pullRequest)
	}()
	return
}

func (self *PullMessageController) pullMessage(pullRequest *model.PullRequest) {
	defaultMQPullConsumer := self.clientFactory.ConsumerTable[pullRequest.ConsumerGroup]
	if pullRequest.ProcessQueue.IsDropped() {
		return
	}

	//pullRequest.ProcessQueue.SetLastPullTimestamp(System.currentTimeMillis());
	// state ok
	// isPause

	delayPullTime := self.needDelayPullMessage(defaultMQPullConsumer, pullRequest)
	if delayPullTime > 0 {
		self.pullMessageLater(pullRequest, delayPullTime)
		return
	}
	commitOffsetValue := defaultMQPullConsumer.offsetStore.ReadOffset(pullRequest.MessageQueue, service.READ_FROM_MEMORY)

	subscriptionData, ok := defaultMQPullConsumer.rebalance.SubscriptionInner[pullRequest.MessageQueue.Topic]
	if !ok {
		self.pullMessageLater(pullRequest, defaultMQPullConsumer.ConsumerConfig.PullTimeDelayMillsWhenException)
		return
	}

	var sysFlag int32 = 0
	if commitOffsetValue > 0 {
		sysFlag |= constant.FLAG_COMMIT_OFFSET
	}
	sysFlag |= constant.FLAG_SUSPEND
	sysFlag |= constant.FLAG_SUBSCRIPTION
	requestHeader := new(header.PullMessageRequestHeader)
	requestHeader.ConsumerGroup = pullRequest.ConsumerGroup
	requestHeader.Topic = pullRequest.MessageQueue.Topic
	requestHeader.QueueId = pullRequest.MessageQueue.QueueId
	requestHeader.QueueOffset = pullRequest.NextOffset

	requestHeader.CommitOffset = commitOffsetValue
	requestHeader.SuspendTimeoutMillis = defaultMQPullConsumer.ConsumerConfig.BrokerSuspendMaxTimeMillis
	requestHeader.MaxMsgNums = int32(defaultMQPullConsumer.ConsumerConfig.PullBatchSize)
	requestHeader.SubVersion = subscriptionData.SubVersion
	requestHeader.Subscription = subscriptionData.SubString

	requestHeader.SysFlag = sysFlag

	pullCallback := func(responseFuture *remoting.ResponseFuture) {
		var nextBeginOffset int64 = pullRequest.NextOffset

		if responseFuture != nil {
			responseCommand := responseFuture.ResponseCommand
			if responseCommand.Code == remoting.SUCCESS && len(responseCommand.Body) > 0 {
				//FOUND
				var err error
				pullResult := responseCommand.ExtFields
				if ok {
					if nextBeginOffsetInter, ok := pullResult["nextBeginOffset"]; ok {
						if nextBeginOffsetStr, ok := nextBeginOffsetInter.(string); ok {
							nextBeginOffset, err = strconv.ParseInt(nextBeginOffsetStr, 10, 64)
							if err != nil {
								glog.Error(err)
								return
							}
						}
					}
				}
				msgs := DecodeMessage(responseFuture.ResponseCommand.Body)

				msgs = FilterMessageAgainByTags(msgs, defaultMQPullConsumer.subscriptionTag[pullRequest.MessageQueue.Topic])
				if len(msgs) == 0 {
					if pullRequest.ProcessQueue.GetMsgCount() == 0 {
						defaultMQPullConsumer.offsetStore.UpdateOffset(pullRequest.MessageQueue, nextBeginOffset, true)
					}
				}
				//
				pullRequest.ProcessQueue.PutMessage(msgs)
				defaultMQPullConsumer.consumeMessageService.SubmitConsumeRequest(msgs, pullRequest.ProcessQueue, pullRequest.MessageQueue, true)
			} else {
				//glog.Error(fmt.Sprintf("pull message error,code=%d,body=%s", responseCommand.Code, string(responseCommand.Body)))
				var err error // change the offset , use nextBeginOffset
				pullResult := responseCommand.ExtFields
				if ok {
					if nextBeginOffsetInter, ok := pullResult["nextBeginOffset"]; ok {
						if nextBeginOffsetStr, ok := nextBeginOffsetInter.(string); ok {
							nextBeginOffset, err = strconv.ParseInt(nextBeginOffsetStr, 10, 64)
							if err != nil {
								glog.Error(err)
							}
						}
					}
				}
				if responseCommand.Code == remoting.PULL_NOT_FOUND || responseCommand.Code == remoting.PULL_RETRY_IMMEDIATELY {
					//NO_NEW_MSG //NO_MATCHED_MSG
					if pullRequest.ProcessQueue.GetMsgCount() == 0 {
						defaultMQPullConsumer.offsetStore.UpdateOffset(pullRequest.MessageQueue, nextBeginOffset, true)
					}
					//update offset increase only
					//failedPullRequest, _ := json.Marshal(pullRequest)
					//glog.Error("the pull request offset illegal", string(failedPullRequest))
				} else if responseCommand.Code == remoting.PULL_OFFSET_MOVED {
					//OFFSET_ILLEGAL
					glog.Error(fmt.Sprintf("PULL_OFFSET_MOVED,code=%d,body=%s", responseCommand.Code, string(responseCommand.Body)))
					pullRequest.ProcessQueue.SetDrop(true)
					go func() {
						executeTaskLater := time.NewTimer(10 * time.Second)
						<-executeTaskLater.C
						defaultMQPullConsumer.offsetStore.UpdateOffset(pullRequest.MessageQueue, nextBeginOffset, false)
						defaultMQPullConsumer.rebalance.RemoveProcessQueue(pullRequest.MessageQueue)
					}()
				} else {
					glog.Errorf("illegal response code. pull message error,code=%d,request=%v OFFSET_ILLEGAL", responseCommand.Code, requestHeader)
					glog.Error(pullRequest.MessageQueue)
					time.Sleep(1 * time.Second)
				}
			}
		} else {
			glog.Error("responseFuture is nil")
		}

		if pullRequest.ProcessQueue.IsDropped() {
			return
		}
		nextPullRequest := &model.PullRequest{
			ConsumerGroup: pullRequest.ConsumerGroup,
			NextOffset:    nextBeginOffset,
			MessageQueue:  pullRequest.MessageQueue,
			ProcessQueue:  pullRequest.ProcessQueue,
		}
		if defaultMQPullConsumer.ConsumerConfig.PullInterval > 0 {
			go func() {
				nextPullTime := time.NewTimer(time.Duration(defaultMQPullConsumer.ConsumerConfig.PullInterval) * time.Millisecond)
				<-nextPullTime.C
				self.mqClient.EnqueuePullMessageRequest(nextPullRequest)
			}()
		} else {
			self.mqClient.EnqueuePullMessageRequest(nextPullRequest)
		}
	}
	glog.V(2).Infof("requestHeader look offset %s %s %s %s", requestHeader.QueueOffset, requestHeader.Topic, requestHeader.QueueId, requestHeader.CommitOffset)
	self.consumerPullMessageAsync(pullRequest.MessageQueue.BrokerName, requestHeader, pullCallback)
}
func FilterMessageAgainByTags(msgExts []model.MessageExt, subscriptionTagList []string) (result []model.MessageExt) {
	result = msgExts
	if len(subscriptionTagList) == 0 {
		return
	}
	result = []model.MessageExt{}
	for _, msg := range msgExts {
		for _, tag := range subscriptionTagList {
			if tag == msg.GetTag() {
				result = append(result, msg)
				break
			}
		}
	}
	return
}

func (self *PullMessageController) consumerPullMessageAsync(brokerName string, requestHeader remoting.CustomerHeader, invokeCallback remoting.InvokeCallback) {
	brokerAddr, _, found := self.mqClient.FindBrokerAddressInSubscribe(brokerName, 0, false)
	if found {
		remotingCommand := remoting.NewRemotingCommand(remoting.PULL_MESSAGE, requestHeader)
		self.mqClient.GetRemotingClient().InvokeAsync(brokerAddr, remotingCommand, 1000, invokeCallback)
	}
}

func DecodeMessage(data []byte) []model.MessageExt {
	buf := bytes.NewBuffer(data)
	var storeSize, magicCode, bodyCRC, queueId, flag, sysFlag, reconsumeTimes, bodyLength, bornPort, storePort int32
	var queueOffset, physicOffset, preparedTransactionOffset, bornTimeStamp, storeTimestamp int64
	var topicLen byte
	var topic, body, properties, bornHost, storeHost []byte
	var propertiesLength int16

	var propertiesmap = make(map[string]string)

	msgs := []model.MessageExt{}
	for buf.Len() > 0 {
		msg := model.MessageExt{Message: &model.Message{}}
		binary.Read(buf, binary.BigEndian, &storeSize)
		binary.Read(buf, binary.BigEndian, &magicCode)
		binary.Read(buf, binary.BigEndian, &bodyCRC)
		binary.Read(buf, binary.BigEndian, &queueId)
		binary.Read(buf, binary.BigEndian, &flag)
		binary.Read(buf, binary.BigEndian, &queueOffset)
		binary.Read(buf, binary.BigEndian, &physicOffset)
		binary.Read(buf, binary.BigEndian, &sysFlag)
		binary.Read(buf, binary.BigEndian, &bornTimeStamp)
		bornHost = make([]byte, 4)
		binary.Read(buf, binary.BigEndian, &bornHost)
		binary.Read(buf, binary.BigEndian, &bornPort)
		binary.Read(buf, binary.BigEndian, &storeTimestamp)
		storeHost = make([]byte, 4)
		binary.Read(buf, binary.BigEndian, &storeHost)
		binary.Read(buf, binary.BigEndian, &storePort)
		binary.Read(buf, binary.BigEndian, &reconsumeTimes)
		binary.Read(buf, binary.BigEndian, &preparedTransactionOffset)
		binary.Read(buf, binary.BigEndian, &bodyLength)
		if bodyLength > 0 {
			body = make([]byte, bodyLength)
			binary.Read(buf, binary.BigEndian, body)
			if (sysFlag & constant.CompressedFlag) == constant.CompressedFlag {
				b := bytes.NewReader(body)
				z, err := zlib.NewReader(b)
				if err != nil {
					glog.Error(err)
					return nil
				}
				body, err = ioutil.ReadAll(z)
				z.Close()
				if err != nil {
					glog.Error(err)
					return nil
				}
			}
		}
		binary.Read(buf, binary.BigEndian, &topicLen)
		topic = make([]byte, int(topicLen))
		binary.Read(buf, binary.BigEndian, &topic)
		binary.Read(buf, binary.BigEndian, &propertiesLength)
		if propertiesLength > 0 {
			properties = make([]byte, propertiesLength)
			binary.Read(buf, binary.BigEndian, &properties)
			propertiesmap = util.String2MessageProperties(string(properties))
		}

		if magicCode != -626843481 {
			glog.Errorf("magic code is error %d", magicCode)
			return nil
		}

		msg.Topic = string(topic)
		msg.QueueId = queueId
		msg.SysFlag = sysFlag
		msg.QueueOffset = queueOffset
		msg.BodyCRC = bodyCRC
		msg.StoreSize = storeSize
		msg.BornTimestamp = bornTimeStamp
		msg.ReconsumeTimes = reconsumeTimes
		msg.Flag = int(flag)
		msg.CommitLogOffset = physicOffset
		msg.StoreTimestamp = storeTimestamp
		msg.PreparedTransactionOffset = preparedTransactionOffset
		msg.Body = body
		msg.Properties = propertiesmap

		//  <  3.5.8 use messageOffsetId
		//  >= 3.5.8 use clientUniqMsgId
		msg.MsgId = msg.GetMsgUniqueKey()
		if len(msg.MsgId) == 0 {
			msg.MsgId = util.GeneratorMessageOffsetId(storeHost, storePort, msg.CommitLogOffset)
		}
		msgs = append(msgs, msg)
	}

	return msgs
}
