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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"sync"
	"time"
)

type MqClientManager struct {
	clientFactory          *ClientFactory
	rocketMqClient         service.RocketMqClient
	pullMessageController  *PullMessageController
	defaultProducerService RocketMQProducer //for send back message

	rocketMqManagerLock sync.Mutex
	//ClientId            string
	BootTimestamp int64

	NamesrvLock   sync.Mutex
	HeartBeatLock sync.Mutex
	//rebalanceControllr       *RebalanceController
}

type MqClientConfig struct {
}

func NewMqClientManager(clientConfig *MqClientConfig) (rocketMqManager *MqClientManager) {
	rocketMqManager = &MqClientManager{}
	rocketMqManager.BootTimestamp = time.Now().Unix()
	rocketMqManager.clientFactory = clientFactoryInit()
	//rocketMqManager.rocketMqClient =
	//rocketMqManager.pullMessageController = NewPullMessageController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	//rocketMqManager.cleanExpireMsgController = NewCleanExpireMsgController(rocketMqManager.mqClient, rocketMqManager.clientFactory)
	//rocketMqManager.rebalanceControllr = NewRebalanceController(rocketMqManager.clientFactory)

	return
}

func (self *MqClientManager) RegisterProducer(producer *DefaultMQProducer) {
	return
}

func (self *MqClientManager) RegisterConsumer(consumer RocketMQConsumer) {
	// todo check config
	//if (self.defaultProducerService == nil) {
	//	self.defaultProducerService = service.NewDefaultProducerService(constant.CLIENT_INNER_PRODUCER_GROUP, mq_config.NewProducerConfig(), self.mqClient)
	//}
	return
}

func (self *MqClientManager) Start() {
	//self.SendHeartbeatToAllBrokerWithLock()//we should send heartbeat first
	self.startAllScheduledTask()
}
func (manager *MqClientManager) startAllScheduledTask() {

}

func clientFactoryInit() (clientFactory *ClientFactory) {
	clientFactory = &ClientFactory{}
	clientFactory.ProducerTable = make(map[string]RocketMQProducer)
	clientFactory.ConsumerTable = make(map[string]RocketMQConsumer)
	return
}

type ClientFactory struct {
	ProducerTable map[string]RocketMQProducer //group|RocketMQProducer
	ConsumerTable map[string]RocketMQConsumer //group|Consumer
}

type PullMessageController struct {
	rocketMqClient service.RocketMqClient
	clientFactory  *ClientFactory
}
