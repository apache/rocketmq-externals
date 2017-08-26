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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel"
)

//MQProducer rocketmq producer
type MQProducer interface {
	//send message,default timeout is 3000
	Send(message rocketmqm.Message) (sendResult *rocketmqm.SendResult, err error)
	//send message with custom timeout
	SendWithTimeout(message rocketmqm.Message, timeout int64) (sendResult *rocketmqm.SendResult, err error)
}

//NewDefaultMQProducer mq producer with default config
func NewDefaultMQProducer(producerGroup string) (r MQProducer) {
	return NewDefaultMQProducerWithCustomConfig(producerGroup, rocketmqm.NewProducerConfig())
}

//NewDefaultMQProducerWithCustomConfig mq producer with custom config
func NewDefaultMQProducerWithCustomConfig(producerGroup string, producerConfig *rocketmqm.MqProducerConfig) (r MQProducer) {
	return kernel.NewDefaultMQProducer(producerGroup, producerConfig)
}
