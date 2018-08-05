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

package kernel

import (
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type mqFaultStrategy struct {
}

//if first select : random one
//if has error broker before ,skip the err broker
func selectOneMessageQueue(topicPublishInfo *model.TopicPublishInfo, lastFailedBroker string) (mqQueue rocketmqm.MessageQueue, err error) {
	queueIndex := topicPublishInfo.FetchQueueIndex()
	queues := topicPublishInfo.MessageQueueList
	if len(lastFailedBroker) == 0 {
		mqQueue = queues[queueIndex]
		return
	}
	for i := 0; i < len(queues); i++ {
		nowQueueIndex := queueIndex + i
		if nowQueueIndex >= len(queues) {
			nowQueueIndex = nowQueueIndex - len(queues)
		}
		if lastFailedBroker == queues[nowQueueIndex].BrokerName {
			continue
		}
		mqQueue = queues[nowQueueIndex]
		return
	}
	err = errors.New("send to [" + lastFailedBroker + "] fail,no other broker")
	return
}
