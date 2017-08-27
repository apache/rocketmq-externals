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

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"

// Message rocketmq message
// topic: the topic of this message
// tag: the topic of this message, one topic may have no tag or different tag
// key: key makes this message easy to search by console (https://github.com/apache/incubator-rocketmq-externals/rocketmq-console)
// body: the message's user content
// see MessageImpl
type Message interface {
	//Topic Topic get topic
	Topic() (topic string)
	//SetTopic set message topic
	SetTopic(tag string)

	//SetTag set message tag
	SetTag(tag string)
	//Tag get message tag
	Tag() (tag string)
	//SetKeys set message key
	SetKeys(keys []string)

	//SetDelayTimeLevel set delay time level
	SetDelayTimeLevel(delayTimeLevel int)
	//SetBody set body
	SetBody([]byte)
}

// NewMessage create a message instance
func NewMessage() (msg Message) {
	msg = message.NewMessageImpl()
	return
}
