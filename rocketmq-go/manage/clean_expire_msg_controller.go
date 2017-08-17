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

package manage

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel"
	"time"
)

type CleanExpireMsgController struct {
	mqClient      kernel.RocketMqClient
	clientFactory *ClientFactory
}

func NewCleanExpireMsgController(mqClient kernel.RocketMqClient, clientFactory *ClientFactory) *CleanExpireMsgController {
	return &CleanExpireMsgController{
		mqClient:      mqClient,
		clientFactory: clientFactory,
	}
}

func (self *CleanExpireMsgController) Start() {
	for _, consumer := range self.clientFactory.ConsumerTable {
		go func() {
			cleanExpireMsgTimer := time.NewTimer(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * 1000 * 60 * time.Millisecond)
			//cleanExpireMsgTimer := time.NewTimer(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * time.Millisecond)
			for {
				<-cleanExpireMsgTimer.C
				consumer.CleanExpireMsg()
				cleanExpireMsgTimer.Reset(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * 1000 * 60 * time.Millisecond)
				//cleanExpireMsgTimer.Reset(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * time.Millisecond)
			}
		}()
	}
}
