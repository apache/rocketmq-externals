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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	_ "net/http/pprof"
)

func main() {
	var (
		testTopic = "GoLang"
	)
	var producer1 = rocketmq.NewDefaultMQProducer("Test1")
	producer1.ProducerConfig.CompressMsgBodyOverHowMuch = 1
	var producer2 = rocketmq.NewDefaultMQProducer("Test2")
	var clienConfig = &config.ClientConfig{}
	clienConfig.SetNameServerAddress("120.55.113.35:9876")
	rocketMqManager := rocketmq.MqClientManagerInit(clienConfig)
	rocketMqManager.RegistProducer(producer1)
	rocketMqManager.RegistProducer(producer2)
	rocketMqManager.Start()
	for i := 0; i < 1000; i++ {
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
