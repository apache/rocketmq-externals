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
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/config"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	//"github.com/golang/glog"
	//"time"
)

func main() {
	//
	//var (
	//	testTopic = "GoLang"
	//)
	//var comsumer1 = rocketmq.NewDefaultMQPushConsumer(testTopic + "-StyleTang")
	//comsumer1.ConsumerConfig.PullInterval = 0
	//comsumer1.ConsumerConfig.ConsumeTimeout = 1
	//comsumer1.ConsumerConfig.ConsumeMessageBatchMaxSize = 16
	//comsumer1.ConsumerConfig.ConsumeFromWhere = "CONSUME_FROM_TIMESTAMP"
	//comsumer1.ConsumerConfig.ConsumeTimestamp = time.Now()
	//comsumer1.Subscribe(testTopic, "*")
	//comsumer1.RegisterMessageListener(func(msgs []rocketmq_api_model.MessageExt) model.ConsumeConcurrentlyResult {
	//	for _, msg := range msgs {
	//		glog.Info(msg.BornTimestamp)
	//	}
	//	glog.Info("look message len ", len(msgs))
	//	return model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: model.CONSUME_SUCCESS, AckIndex: len(msgs)}
	//})
	//var clienConfig = &rocketmq_api_model.ClientConfig{}
	//clienConfig.SetNameServerAddress("127.0.0.1:9876")
	//rocketMqManager := rocketmq.MqClientManagerInit(clienConfig)
	//rocketMqManager.RegistConsumer(comsumer1)
	//rocketMqManager.Start()
	//select {}
	//rocketMqManager.ShutDown()
}
