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
		testProducerGroup = "TestDelayProducerGroup"
		testConsumerGroup = "TestDelayConsumerGroup"
		tag               = "TestDelayMessageTag"
	)
	var messageId string
	var startTime time.Time
	chResult := make(chan bool, 1)
	rocketMQClientInstance := rocketmq.InitRocketMQClientInstance(nameServerAddress)
	var producer = rocketmq.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	consumer.RegisterMessageListener(func(messageList []rocketmqm.MessageExt) rocketmqm.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			endTime := time.Now()
			if msg.MsgId() != messageId {
				panic("messageId is wrong " + msg.MsgId())
			}
			costSeconds := endTime.Unix() - startTime.Unix()
			if costSeconds < 14 || costSeconds > 16 {
				panic("delay time message is error ")
			}
			chResult <- true
			successIndex = index

		}
		return rocketmqm.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmqm.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	<-time.After(time.Second * 30) // wait
	var message = rocketmqm.NewMessage()
	message.SetTopic(testTopic)
	message.SetBody([]byte("hello world"))
	message.SetTag(tag)
	message.SetDelayTimeLevel(3) // cost 15 second
	result, err := producer.Send(message)
	startTime = time.Now()
	messageId = result.MsgID()
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	select {
	case <-chResult:
	case <-time.After(time.Second * 30):
		panic("receive tag message timeout")
	}
	glog.Info("Test tag message success")

}
