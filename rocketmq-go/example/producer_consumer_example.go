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
package main

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go" //todo todo  I want only import this
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"net/http"
	_ "net/http/pprof"
	"time"
)

func main() {
	go func() {
		http.ListenAndServe("localhost:6060", nil)
	}()
	var (
		testTopic = "GoLang"
	)
	var producer1 = rocketmq.NewDefaultMQProducer("Test1")
	producer1.ProducerConfig.CompressMsgBodyOverHowMuch = 1
	var producer2 = rocketmq.NewDefaultMQProducer("Test2")
	var comsumer1 = rocketmq.NewDefaultMQPushConsumer(testTopic + "-StyleTang")
	comsumer1.ConsumerConfig.PullInterval = 0
	comsumer1.ConsumerConfig.ConsumeTimeout = 1
	comsumer1.ConsumerConfig.ConsumeMessageBatchMaxSize = 16
	comsumer1.ConsumerConfig.ConsumeFromWhere = "CONSUME_FROM_TIMESTAMP"
	comsumer1.ConsumerConfig.ConsumeTimestamp = time.Now()
	comsumer1.Subscribe(testTopic, "*")
	comsumer1.RegisterMessageListener(func(msgs []model.MessageExt) model.ConsumeConcurrentlyResult {
		for _, msg := range msgs {
			glog.Info(msg.BornTimestamp)
		}
		glog.Info("look message len ", len(msgs))
		return model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: model.CONSUME_SUCCESS, AckIndex: len(msgs)}
	})
	var clienConfig = &config.ClientConfig{}
	clienConfig.SetNameServerAddress("127.0.0.1:9876")
	rocketMqManager := rocketmq.MqClientManagerInit(clienConfig)
	rocketMqManager.RegistProducer(producer1)
	rocketMqManager.RegistProducer(producer2)
	rocketMqManager.RegistConsumer(comsumer1)
	rocketMqManager.Start()
	for i := 0; i < 10000000; i++ {
		var message = &model.Message{}
		message.Topic = testTopic
		message.SetKeys([]string{"xxx"})
		message.SetTag("1122")
		message.Body = []byte("hellAXXWord" + util.IntToString(i))

		xx, ee := producer1.Send(message)
		if ee != nil {
			glog.Error(ee)
			continue
		}
		glog.V(0).Infof("sendMessageResutl messageId[%s] err[%s]", xx.MsgID(), ee)
	}
	select {}
	rocketMqManager.ShutDown()
}
