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
package message

type MessageQueue struct {
	topic      string
	brokerName string
	queueId    int32
}

func NewMessageQueue(topic string, brokerName string, queueId int32) *MessageQueue {
	return &MessageQueue{
		topic:      topic,
		brokerName: brokerName,
		queueId:    queueId,
	}
}

func (queue *MessageQueue) clone() *MessageQueue {
	no := new(MessageQueue)
	no.topic = queue.topic
	no.queueId = queue.queueId
	no.brokerName = queue.brokerName
	return no
}

func (queue MessageQueue) BrokerName() string {
	return queue.brokerName
}

func (queue *MessageQueue) QueueID() int32 {
	return queue.queueId
}

type MessageQueues []*MessageQueue

func (queues MessageQueues) Less(i, j int) bool {
	imq := queues[i]
	jmq := queues[j]

	if imq.topic < jmq.topic {
		return true
	}

	if imq.topic < jmq.topic {
		return false
	}

	if imq.brokerName < jmq.brokerName {
		return true
	}

	if imq.brokerName < jmq.brokerName {
		return false
	}

	if imq.queueId < jmq.queueId {
		return true
	}

	return false
}

func (queues MessageQueues) Swap(i, j int) {
	queues[i], queues[j] = queues[j], queues[i]
}

func (queues MessageQueues) Len() int {
	return len(queues)
}
