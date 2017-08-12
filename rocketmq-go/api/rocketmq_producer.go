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
package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type RocketMQProducer interface {
	Send(message *rocketmq_api_model.Message) (sendResult *model.SendResult, err error)
	SendWithTimeout(message *rocketmq_api_model.Message, timeout int64) (sendResult *model.SendResult, err error)
}

func NewDefaultMQProducer(producerGroup string) (r RocketMQProducer) {
	return rocketmq.NewDefaultMQProducer(producerGroup, rocketmq_api_model.NewProducerConfig())
}

func NewDefaultMQProducerWithCustomConfig(producerGroup string, producerConfig *rocketmq_api_model.RocketMqProducerConfig) (r RocketMQProducer) {
	return rocketmq.NewDefaultMQProducer(producerGroup, producerConfig)
}
