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
)

func main() {
	var (
		nameServerAddress = "127.0.0.1:9876" //address split by ;  (for example 192.168.1.1:9876;192.168.1.2:9876)
		testTopic         = "GoLangRocketMQ"
		testConsumerGroup = "TestConsumerGroup"
	)
	// init rocketMQClientInstance
	rocketMQClientInstance := rocketmq.InitRocketMQClientInstance(nameServerAddress)
	// 1.init rocketMQConsumer
	// 2.subscribe topic and register our function to message listener
	// 3.register it
	var consumer = rocketmq.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, "*")
	consumer.RegisterMessageListener(func(messageList []rocketmqm.MessageExt) rocketmqm.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			glog.Infof("test receiveMessage messageId=[%s] messageBody=[%s]", msg.MsgId(), string(msg.Body()))
			// call your function
			successIndex = index
		}
		return rocketmqm.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmqm.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)

	// start rocketMQ client instance
	rocketMQClientInstance.Start()
	select {}
}
