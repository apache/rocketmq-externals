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
		testProducerGroup = "TestProducerGroup"
	)
	// init rocketMQClientInstance
	rocketMQClientInstance := rocketmq.InitRocketMQClientInstance(nameServerAddress)
	// init rocketMQProducer and register it
	var producer = rocketmq.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)

	// start rocketMQ client instance
	rocketMQClientInstance.Start()

	//start send test message
	var message = rocketmqm.NewMessage()
	message.SetTopic(testTopic)
	message.SetBody([]byte("hello World"))
	result, err := producer.Send(message)
	glog.Infof("test sendMessageResult result=[%s] err=[%s]", result.String(), err)
}
