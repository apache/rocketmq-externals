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

package rocketmqm

//MessageQueue message queue
type MessageQueue struct {
	Topic      string `json:"topic"`
	BrokerName string `json:"brokerName"`
	QueueId    int32  `json:"queueId"`
}

//Equals judge messageQueue is the same
func (m MessageQueue) Equals(messageQueue *MessageQueue) bool {
	if m.QueueId != messageQueue.QueueId {
		return false
	}
	if m.Topic != messageQueue.Topic {
		return false
	}
	if m.BrokerName != messageQueue.BrokerName {
		return false
	}
	return true
}
