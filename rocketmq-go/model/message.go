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
package model

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"strconv"
	"strings"
)

type Message struct {
	Topic      string
	Flag       int
	Properties map[string]string
	Body       []byte
}

func (self *Message) SetTag(tag string) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_TAGS] = tag
}
func (self *Message) GetTag() (tag string) {
	if self.Properties != nil {
		tag = self.Properties[constant.PROPERTY_TAGS]
	}
	return
}

func (self *Message) SetKeys(keys []string) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_KEYS] = strings.Join(keys, KEY_SEPARATOR)
}

func (self *Message) SetDelayTimeLevel(delayTimeLevel int) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_DELAY_TIME_LEVEL] = util.IntToString(delayTimeLevel)
}
func (self *Message) SetWaitStoreMsgOK(waitStoreMsgOK bool) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_WAIT_STORE_MSG_OK] = strconv.FormatBool(waitStoreMsgOK)
}
func (self *Message) GeneratorMsgUniqueKey() {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	if len(self.Properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX]) > 0 {
		return
	}
	self.Properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX] = util.GeneratorMessageClientId()
}

func (self *MessageExt) GetMsgUniqueKey() string {
	if self.Properties != nil {
		originMessageId := self.Properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX]
		if len(originMessageId) > 0 {
			return originMessageId
		}
	}
	return self.MsgId
}

func (self *Message) SetOriginMessageId(messageId string) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_ORIGIN_MESSAGE_ID] = messageId
}

func (self *Message) SetRetryTopic(retryTopic string) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_RETRY_TOPIC] = retryTopic
}
func (self *Message) SetReconsumeTime(reConsumeTime int) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_RECONSUME_TIME] = util.IntToString(reConsumeTime)
}

func (self *Message) GetReconsumeTimes() (reConsumeTime int) {
	reConsumeTime = 0
	if self.Properties != nil {
		reConsumeTimeStr := self.Properties[constant.PROPERTY_RECONSUME_TIME]
		if len(reConsumeTimeStr) > 0 {
			reConsumeTime = util.StrToIntWithDefaultValue(reConsumeTimeStr, 0)
		}
	}
	return
}

func (self *Message) SetMaxReconsumeTimes(maxConsumeTime int) {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	self.Properties[constant.PROPERTY_MAX_RECONSUME_TIMES] = util.IntToString(maxConsumeTime)
}

func (self *Message) GetMaxReconsumeTimes() (maxConsumeTime int) {
	maxConsumeTime = 0
	if self.Properties != nil {
		reConsumeTimeStr := self.Properties[constant.PROPERTY_MAX_RECONSUME_TIMES]
		if len(reConsumeTimeStr) > 0 {
			maxConsumeTime = util.StrToIntWithDefaultValue(reConsumeTimeStr, 0)
		}
	}
	return
}

var KEY_SEPARATOR string = " "
