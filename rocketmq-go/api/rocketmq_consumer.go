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

//MQConsumer rocketmq consumer
type MQConsumer interface {
	// register custom's message listener to this consumer
	RegisterMessageListener(listener rocketmqm.MessageListener)

	// this consumer subscribe which topic, filter tags with subExpression
	// subExpression is split by |
	// for example.
	// consume topic "TestTopic1",consume all message tag
	// mqConsumer.Subscribe("TestTopic1","*")
	// consume topic "TestTopic2",consume message with tag1 or tag2
	// mqConsumer.Subscribe("TestTopic2","tag1|tag2")
	Subscribe(topic string, subExpression string)
}

//NewDefaultMQPushConsumer Concurrently(no order) CLUSTERING mq consumer with default config
func NewDefaultMQPushConsumer(producerGroup string) (r MQConsumer) {
	return NewDefaultMQPushConsumerWithCustomConfig(producerGroup, rocketmqm.NewRocketMqConsumerConfig())
}

//NewDefaultMQPushConsumerWithCustomConfig Concurrently(no order) CLUSTERING mq consumer with custom config
func NewDefaultMQPushConsumerWithCustomConfig(producerGroup string, consumerConfig *rocketmqm.MqConsumerConfig) (r MQConsumer) {
	return kernel.NewDefaultMQPushConsumer(producerGroup, consumerConfig)
}
