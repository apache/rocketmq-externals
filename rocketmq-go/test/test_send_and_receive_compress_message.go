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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/golang/glog"
	"time"
)

func main() {
	chResult := make(chan bool, 1)
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestCompressProducerGroup"
		testConsumerGroup = "TestCompressConsumerGroup"
	)
	var bigMessageBody = "test_string"
	for i := 0; i < 16; i++ {
		bigMessageBody += bigMessageBody
	}
	//bigMessageBody len will be 720896,it will be compressed
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	producerConfig := rocketmq_api_model.NewProducerConfig()
	producerConfig.CompressMsgBodyOverHowMuch = 500
	var producer = rocketmq_api.NewDefaultMQProducerWithCustomConfig(testProducerGroup, producerConfig)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, "compress_message_test")
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			if msg.SysFlag&constant.CompressedFlag != constant.CompressedFlag {
				panic("message not be compressed")
			}
			if string(msg.Body) != bigMessageBody {
				panic("message not be unCompressed")
			}
			glog.Info("Test compress and tag success")
			successIndex = index

		}
		chResult <- true
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte(bigMessageBody)}
	message.SetTag("compress_message_test")
	result, err := producer.Send(message)
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	select {
	case <-chResult:
	case <-time.After(time.Second * 30):
		panic("receive compressed message timeout")
	}
}
