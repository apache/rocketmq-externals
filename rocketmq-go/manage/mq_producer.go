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

package manage

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel"
)

type DefaultMQProducer struct {
	producerGroup  string
	ProducerConfig *rocketmqm.MqProducerConfig

	producerService kernel.ProducerService
}

func NewDefaultMQProducer(producerGroup string, producerConfig *rocketmqm.MqProducerConfig) (rocketMQProducer *DefaultMQProducer) {
	rocketMQProducer = &DefaultMQProducer{
		producerGroup:  producerGroup,
		ProducerConfig: producerConfig,
	}
	return
}

func (self *DefaultMQProducer) Send(message *rocketmqm.MessageImpl) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", self.ProducerConfig.SendMsgTimeout)
	return
}
func (self *DefaultMQProducer) SendWithTimeout(message *rocketmqm.MessageImpl, timeout int64) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", timeout)
	return
}
