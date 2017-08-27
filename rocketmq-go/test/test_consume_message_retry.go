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

//test consume message, first and second time consume error,third time consume success
func main() {
	chResult := make(chan bool, 3)
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestRetryProducerGroup"
		testConsumerGroup = "TestRetryConsumerGroup"
		tag               = "RetryTestTag"
		testMessageBody   = "RetryTestMessageBody"
		consumeTime       = 0
	)
	rocketMQClientInstance := rocketmq.InitRocketMQClientInstance(nameServerAddress)
	var producer = rocketmq.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	consumer.RegisterMessageListener(func(messageList []rocketmqm.MessageExt) rocketmqm.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, message := range messageList {
			if string(message.Body()) != testMessageBody {
				panic("message.body is wrong message.body=" + string(message.Body()) + " testMessageBody=" + testMessageBody + " tag=" + message.Tag())
			}
			if consumeTime < 2 {
				consumeTime++
				chResult <- true
				glog.Info("test consume fail")
				break
			}
			glog.Info("test consume success")
			chResult <- true
			successIndex = index
		}
		return rocketmqm.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmqm.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	var message = rocketmqm.NewMessage()
	message.SetTopic(testTopic)
	message.SetBody([]byte(testMessageBody))
	message.SetTag(tag)
	result, err := producer.Send(message)
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	for i := 0; i < 3; i++ {
		select {
		case <-chResult:
		case <-time.After(time.Second * 50):
			panic("receive tag message timeout")
		}
	}
	glog.Info("Test tag message success")
}
