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

package rocketmq

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
)

type MQClientInstance interface {
	RegisterProducer(producer MQProducer)
	RegisterConsumer(consumer MQConsumer)
	Start()
}

type RocketMQClientInstanceImpl struct {
	rocketMqManager *manage.MqClientManager
}

func InitRocketMQClientInstance(nameServerAddress string) (rocketMQClientInstance MQClientInstance) {
	mqClientConfig := rocketmqm.NewMqClientConfig(nameServerAddress)
	return InitRocketMQClientInstanceWithCustomClientConfig(mqClientConfig)
}
func InitRocketMQClientInstanceWithCustomClientConfig(mqClientConfig *rocketmqm.MqClientConfig) (rocketMQClientInstance MQClientInstance) {
	rocketMQClientInstance = &RocketMQClientInstanceImpl{rocketMqManager: manage.MqClientManagerInit(mqClientConfig)}
	return
}

func (r *RocketMQClientInstanceImpl) RegisterProducer(producer MQProducer) {
	r.rocketMqManager.RegistProducer(producer.(*manage.DefaultMQProducer))
}

func (r *RocketMQClientInstanceImpl) RegisterConsumer(consumer MQConsumer) {
	r.rocketMqManager.RegistConsumer(consumer.(*manage.DefaultMQPushConsumer))
}
func (r *RocketMQClientInstanceImpl) Start() {
	r.rocketMqManager.Start()
}
