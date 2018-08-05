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

package message

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"strings"
)

//MessageImpl the implement of Message
type MessageImpl struct {
	topic      string
	flag       int
	properties map[string]string
	body       []byte
}

//NewMessageImpl create a Message instance
func NewMessageImpl() (message *MessageImpl) {
	message = &MessageImpl{}
	return
}

//Properties get message's Properties
func (m *MessageImpl) Properties() (properties map[string]string) {
	properties = m.properties
	return
}

//SetProperties set message's Properties
func (m *MessageImpl) SetProperties(properties map[string]string) {
	m.properties = properties
	return
}

//PropertiesKeyValue get properties[key]'s value
func (m *MessageImpl) PropertiesKeyValue(key string) (value string) {
	value = m.properties[key]
	return
}

//Body get message body
func (m *MessageImpl) Body() (body []byte) {
	body = m.body
	return

}

//Topic get message Topic
func (m *MessageImpl) Topic() (topic string) {
	topic = m.topic
	return

}

//SetFlag set message flag
func (m *MessageImpl) SetFlag(flag int) {
	m.flag = flag
	return
}

//Flag get message flag
func (m *MessageImpl) Flag() (flag int) {
	flag = m.flag
	return

}

//SetTopic set topic
func (m *MessageImpl) SetTopic(topic string) {
	m.topic = topic
}

//SetBody set body
func (m *MessageImpl) SetBody(body []byte) {
	m.body = body
}

//SetTag set message tag
func (m *MessageImpl) SetTag(tag string) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_TAGS] = tag
}

//Tag get message tag from properties
func (m *MessageImpl) Tag() (tag string) {
	if m.properties != nil {
		tag = m.properties[constant.PROPERTY_TAGS]
	}
	return
}

//SetKeys set message key
func (m *MessageImpl) SetKeys(keys []string) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_KEYS] = strings.Join(keys, " ")
}

//SetDelayTimeLevel set message delay time level
func (m *MessageImpl) SetDelayTimeLevel(delayTimeLevel int) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_DELAY_TIME_LEVEL] = util.IntToString(delayTimeLevel)
}

//GeneratorMsgUniqueKey only use by system
func (m *MessageImpl) GeneratorMsgUniqueKey() {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	if len(m.properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX]) > 0 {
		return
	}
	m.properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX] = generatorMessageClientId()
}

//GetMsgUniqueKey only use by system
func (m *MessageExtImpl) GetMsgUniqueKey() string {
	if m.properties != nil {
		originMessageId := m.properties[constant.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX]
		if len(originMessageId) > 0 {
			return originMessageId
		}
	}
	return m.msgId
}

//SetOriginMessageId only use by system
func (m *MessageImpl) SetOriginMessageId(messageId string) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_ORIGIN_MESSAGE_ID] = messageId
}

//SetRetryTopic only use by system
func (m *MessageImpl) SetRetryTopic(retryTopic string) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_RETRY_TOPIC] = retryTopic
}

//SetReconsumeTime only use by system
func (m *MessageImpl) SetReconsumeTime(reConsumeTime int) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_RECONSUME_TIME] = util.IntToString(reConsumeTime)
}

//GetReconsumeTimes only use by system
func (m *MessageImpl) GetReconsumeTimes() (reConsumeTime int) {
	reConsumeTime = 0
	if m.properties != nil {
		reConsumeTimeStr := m.properties[constant.PROPERTY_RECONSUME_TIME]
		if len(reConsumeTimeStr) > 0 {
			reConsumeTime = util.StrToIntWithDefaultValue(reConsumeTimeStr, 0)
		}
	}
	return
}

//SetMaxReconsumeTimes only use by system
func (m *MessageImpl) SetMaxReconsumeTimes(maxConsumeTime int) {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	m.properties[constant.PROPERTY_MAX_RECONSUME_TIMES] = util.IntToString(maxConsumeTime)
}

//GetMaxReconsumeTimes only use by system
func (m *MessageImpl) GetMaxReconsumeTimes() (maxConsumeTime int) {
	maxConsumeTime = 0
	if m.properties != nil {
		reConsumeTimeStr := m.properties[constant.PROPERTY_MAX_RECONSUME_TIMES]
		if len(reConsumeTimeStr) > 0 {
			maxConsumeTime = util.StrToIntWithDefaultValue(reConsumeTimeStr, 0)
		}
	}
	return
}
