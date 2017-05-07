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
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/golang/glog"
)

func main() {

	// create a mqClientManager instance
	var mqClientConfig = &rocketmq.MqClientConfig{}
	var mqClientManager = rocketmq.NewMqClientManager(mqClientConfig)

	// create rocketMq consumer
	var consumerConfig = &rocketmq.MqConsumerConfig{}
	var consumer1 = rocketmq.NewDefaultMQPushConsumer("testGroup", consumerConfig)
	consumer1.Subscribe("testTopic", "*")
	consumer1.RegisterMessageListener(func(msgs []model.MessageExt) model.ConsumeConcurrentlyResult {
		var index = -1
		for i, msg := range msgs {
			// your code here,for example,print msg
			glog.Info(msg)
			var err = errors.New("error")
			if err != nil {
				break
			}
			index = i
		}
		return model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: model.CONSUME_SUCCESS, AckIndex: index}
	})

	//register consumer to mqClientManager
	mqClientManager.RegisterConsumer(consumer1)

	//start it
	mqClientManager.Start()
}
