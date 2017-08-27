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

package main

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/golang/glog"
	"time"
)

func main() {
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestSerializeProducerGroup"
		testConsumerGroup = "TestSerializeConsumerGroup"
		tag               = "TestSerializeMessageTag"
		messageBody       = "testMessageBody_testMessageBody"
		messageCount      = 100
	)
	chResult := make(chan bool, messageCount)
	mqClientConfig := rocketmqm.NewMqClientConfig(nameServerAddress)
	mqClientConfig.ClientSerializeType = rocketmqm.ROCKETMQ_SERIALIZE
	rocketMQClientInstance := rocketmq.InitRocketMQClientInstanceWithCustomClientConfig(mqClientConfig)
	var producer = rocketmq.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	consumer.RegisterMessageListener(func(messageList []rocketmqm.MessageExt) rocketmqm.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			if msg.Tag() == tag && messageBody == string(messageBody) {
				chResult <- true
			}
			successIndex = index

		}
		return rocketmqm.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmqm.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	for i := 0; i < messageCount; i++ {
		var message = rocketmqm.NewMessage()
		message.SetTopic(testTopic)
		message.SetBody([]byte(messageBody))
		message.SetTag(tag)
		result, err := producer.Send(message)
		glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	}
	for i := 0; i < messageCount; i++ {
		select {
		case <-chResult:
		case <-time.After(time.Second * 30):
			panic("receive tag message timeout")
		}
	}
	glog.Info("Test tag message success")

}
