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

package kernel

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"

	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
)

//DefaultMQProducer use DefaultMQProducer send message
type DefaultMQProducer struct {
	producerGroup  string
	ProducerConfig *rocketmqm.MqProducerConfig

	producerService ProducerService
}

//NewDefaultMQProducer create a  DefaultMQProducer instance
// see rocketmq_producer.go
func NewDefaultMQProducer(producerGroup string, producerConfig *rocketmqm.MqProducerConfig) (rocketMQProducer *DefaultMQProducer) {
	rocketMQProducer = &DefaultMQProducer{
		producerGroup:  producerGroup,
		ProducerConfig: producerConfig,
	}
	return
}

//Send see rocketmq_producer.go
func (d *DefaultMQProducer) Send(msg rocketmqm.Message) (sendResult *rocketmqm.SendResult, err error) {
	sendResult, err = d.producerService.sendDefaultImpl(msg.(*message.MessageImpl), constant.COMMUNICATIONMODE_SYNC, "", d.ProducerConfig.SendMsgTimeout)
	return
}

//SendWithTimeout see rocketmq_producer.go
func (d *DefaultMQProducer) SendWithTimeout(msg rocketmqm.Message, timeout int64) (sendResult *rocketmqm.SendResult, err error) {
	sendResult, err = d.producerService.sendDefaultImpl(msg.(*message.MessageImpl), constant.COMMUNICATIONMODE_SYNC, "", timeout)
	return
}
