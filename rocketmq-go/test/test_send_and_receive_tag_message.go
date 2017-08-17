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
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"time"
)

func main() {
	chResult := make(chan bool, 3)
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestTagProducerGroup"
		testConsumerGroup = "TestTagConsumerGroup"
	)
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	var producer = rocketmq_api.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, "tag0 || tag2||tag4")
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			if msg.GetTag() != "tag0" && msg.GetTag() != "tag2" && msg.GetTag() != "tag4" {
				panic("receive message not belong here tag=" + msg.GetTag())
			}
			fmt.Println("got " + msg.GetTag())
			chResult <- true
			successIndex = index

		}
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	for i := 0; i < 5; i++ {
		var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte("hello world")}
		message.SetTag("tag" + util.IntToString(i))
		result, err := producer.Send(message)
		glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	}
	for i := 0; i < 3; i++ {
		select {
		case <-chResult:

		case <-time.After(time.Second * 30):
			panic("receive tag message timeout")
		}
	}
	glog.Info("Test tag message success")

}
