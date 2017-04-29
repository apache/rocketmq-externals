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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type MQConsumer interface {
	CreateTopic(key, newTopic string, queueNum, topicSysFlag int) error
	SearchOffset(mq *message.MessageQueue, timeStamp time.Time) (int64, error)
	MaxOffset(mq *message.MessageQueue)(int64, error)
	MinOffset(mq *message.MessageQueue)(int64, error)
	EarliestMsgStoreTime(mq *message.MessageQueue) (int64, error)
	ViewMessage(msgID, topic string) (*message.MessageExt,error)
	QueryMessage(topic, key string, maxNum int, begin, end time.Time) (*model.QueryResult, error)

	SendMessageBack(msgExt *message.MessageExt, delayLevel int, brokerName string) error
	FetchSubscribeMessageQueues(topic string) error
}

type MQPushConsumer interface {
	MQConsumer
	Start() error
	Shutdown()
	RegisterMessageListener(listener interface{})
	Subscribe(topic, subExpression string, opts ...interface{}) error
 }

type MqConsumerConfig struct {
	clientConfig *config.ClientConfig
}

type MessageModel int

const (
	Broadcasting MessageModel = iota
	Clustering
)

type ConsumeFromWhere int

const (
	ConsumeFromLastOffset ConsumeFromWhere = iota
	ConsumeFromFirstOffset
	ConsumeFromTimestamp
)

type DefaultMQPushConsumer struct { // 直接按照impl写
	mqClient              service.RocketMqClient
	consumeMessageService service.ConsumeMessageService
	//ConsumerConfig        *MqConsumerConfig
	clientConfig     *config.ClientConfig
	consumerGroup    string
	messageModel     MessageModel
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

func CreateMQPushConsumer() *DefaultMQPushConsumer {
	return &DefaultMQPushConsumer{
		mqClient:              nil,
		consumeMessageService: nil,
		clientConfig:          nil,
		consumerGroup:         "default",
		messageModel:          Clustering,
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

func (dc *DefaultMQPushConsumer) Start() error {
	return nil
}

func (dc *DefaultMQPushConsumer) Shutdown() {
	dc.quit <- 0
}

func (dc *DefaultMQPushConsumer) RegisterMessageListener(listener interface{})

func (dc *DefaultMQPushConsumer) Subscribe(topic, subExpression string, opts ...interface{}) error

func (dc *DefaultMQPushConsumer) CreateTopic(key, newTopic string, queueNum, topicSysFlag int) error

func (dc *DefaultMQPushConsumer) SearchOffset(mq *message.MessageQueue, timeStamp time.Time) (int64, error)

func (dc *DefaultMQPushConsumer) MaxOffset(mq *message.MessageQueue)(int64, error)

func (dc *DefaultMQPushConsumer) MinOffset(mq *message.MessageQueue)(int64, error)

func (dc *DefaultMQPushConsumer) EarliestMsgStoreTime(mq *message.MessageQueue) (int64, error)

func (dc *DefaultMQPushConsumer) ViewMessage(msgID, topic string) (*message.MessageExt,error)

func (dc *DefaultMQPushConsumer) QueryMessage(topic, key string, maxNum int, begin, end time.Time) (*model.QueryResult, error)


func (dc *DefaultMQPushConsumer) SendMessageBack(msgExt *message.MessageExt, delayLevel int, brokerName string) error

func (dc *DefaultMQPushConsumer) FetchSubscribeMessageQueues(topic string) error

func (dc *DefaultMQPushConsumer) makeSureStatusOK() error {

}