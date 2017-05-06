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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"time"
)

const (
	PullTimeDelayMillsWhenError time.Duration = 3 * time.Second
	// Flow control interval
	PullTimeDelayMillsWhenFlowControl time.Duration = 50 * time.Millisecond
	// Delay some time when suspend pull service
	PullTimeDelayMillsWhenSuspend   time.Duration = 1 * time.Second
	BrokerSuspendMaxTimeMills       time.Duration = 15 * time.Second
	ConsumerTimeoutMillsWhenSuspend time.Duration = 30 * time.Second
)

type ServiceStatus int

func (s ServiceStatus) String() string {
	switch s {
	case CreateJust:
		return "CreateJust"
	case Running:
		return "Running"
	case ShutdownAlready:
		return "ShutdownAlready"
	case StartFailed:
		return "StartFailed"
	}
	return "unknow ServiceStatus"
}

const (
	CreateJust ServiceStatus = iota
	Running
	ShutdownAlready
	StartFailed
)

type ConsumeFromWhere int

func (c ConsumeFromWhere) String() string {
	switch c {
	case ConsumeFromLastOffset:
		return "ConsumeFromLastOffset"
	case ConsumeFromFirstOffset:
		return "ConsumeFromFirstOffset"
	case ConsumeFromTimestamp:
		return "ConsumeFromTimestamp"
	}
	return "unknow ConsumeFromWhere"
}

const (
	ConsumeFromLastOffset ConsumeFromWhere = iota
	ConsumeFromFirstOffset
	ConsumeFromTimestamp
)

type MQConsumer interface {
	SendMessageBack(msgX *message.MessageExt, delayLevel int, brokerName string) error
	FetchSubscribeMessageQueues(topic string) ([]*message.MessageQueue, error)

	groupName() string
	messageModel() service.MessageModel
	consumeType() service.ConsumeType
	consumeFromWhere() ConsumeFromWhere
	doRebalance()
	persistConsumerOffset()
	updateTopicSubscribeInfo(topic string, info []*message.MessageQueue)
	unitMode() bool
	runningInfo() runningInfo
}

type MQPushConsumer struct {
	//filterMessageHookList
	consumerStartTimestamp time.Time
	//consumeMessageHookList
	rpcHook remoting.RPCHook
	status  ServiceStatus
	//	mQClientFactory
	//pullAPIWrapper
	pause          bool
	consumeOrderly bool
	//messageListenerInner
	flowControlTimes1 int64
	flowControlTimes2 int64

	api *service.MQClientAPI
}

type DefaultMQPushConsumer struct { // 直接按照impl写
	mqClient  service.RocketMqClient
	clientAPI *service.MQClientAPI

	consumeMessageService service.ConsumeMessageService
	//ConsumerConfig        *MqConsumerConfig
	clientConfig     *config.ClientConfig
	consumerGroup    string
	messageModel     service.MessageModel
	consumeFromWhere ConsumeFromWhere
	consumeTimestamp time.Time
	// AllocateMessageQueueStrategy
	rebalance     *service.Rebalance //Rebalance's impl depend on offsetStore
	subscriptions map[string]string
	// TODO MessageListener
	offsetStore service.OffsetStore //for consumer's offset

	consumeConcurrentMin       int
	consumeConcurrentMax       int
	adjustChannelSizeThreshold int
	consumeConcurrentlyMaxSpan int
	pullThresholdForQueue      int
	pullInterval               time.Duration
	consumeMessageBatchMaxSize int
	pullBatchSize              int
	postSubscriptionWhenPull   bool
	unitMode                   bool
	maxReconsumeTimes          int
	// TODO queue -> chan?
	suspendCurrentQueueTimeMillis time.Duration
	consumeTimeout                time.Duration

	quit chan int
}

func NewMQPushConsumer() MQPushConsumer {
	return nil
}

func NewDefaultPushConsumer() *DefaultMQPushConsumer {
	return &DefaultMQPushConsumer{
		mqClient: nil,
		// TODO new API
		consumeMessageService: nil,
		clientConfig:          nil,
		consumerGroup:         "default",
		messageModel:          service.Clustering,
		consumeFromWhere:      ConsumeFromLastOffset,
		consumeTimestamp:      time.Now(), // TODO get from env
		// AllocateMessageQueueStrategy
		rebalance:     nil,
		subscriptions: make(map[string]string),
		// TODO MessageListener
		offsetStore: nil,

		consumeConcurrentMin:       20,
		consumeConcurrentMax:       64,
		adjustChannelSizeThreshold: 1000,
		consumeConcurrentlyMaxSpan: 2000,
		pullThresholdForQueue:      1000,
		pullInterval:               0,
		consumeMessageBatchMaxSize: 1,
		pullBatchSize:              32,
		postSubscriptionWhenPull:   false,
		unitMode:                   false,
		maxReconsumeTimes:          -1,
		// TODO queue -> chan?
		suspendCurrentQueueTimeMillis: 1 * time.Second,
		consumeTimeout:                15 * time.Millisecond,

		quit: make(chan int),
	}
}

func (dpc *MQPushConsumer) CreateTopic(key, topic string, queueNum, topicSysFlag int) error {
	// TODO
	//return dpc.api.CreateTopic(key, topic, queueNum, topicSysFlag)
	return nil
}

func (dpc *MQPushConsumer) SendMessageBack(msgX *message.MessageExt, delayLevel int, brokerName string) error {
	return nil
}

func (dpc *MQPushConsumer) FetchSubscribeMessageQueues(topic string) ([]*message.MessageQueue, error) {
	//result := dpc.rebalance.TopicSubscribeInfoTable()[topic]
	//
	//if result == nil {
	//	dpc.api.TopicRouteInfoFromNameServer(topic, 3 * time.Second)
	//	result = dpc.rebalance.TopicSubscribeInfoTable()[topic]
	//}
	//
	//if result == nil {
	//	return nil, errors.New(fmt.Sprintf("The topic %s not exist", topic))
	//}
	return nil, nil
}

func (dpc *MQPushConsumer) makeSureStatusOK() error {
	return nil
}
