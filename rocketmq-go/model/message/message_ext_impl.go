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
	"math"
)

//MessageExtImpl the implement of MessageExt
type MessageExtImpl struct {
	*MessageImpl
	msgId                         string
	QueueId                       int32
	StoreSize                     int32
	QueueOffset                   int64
	SysFlag                       int32
	BornTimestamp                 int64
	BornHost                      string
	StoreTimestamp                int64
	StoreHost                     string
	CommitLogOffset               int64
	BodyCRC                       int32
	ReconsumeTimes                int32
	PreparedTransactionOffset     int64
	propertyConsumeStartTimestamp string
}

//MsgId get MessageId
func (m *MessageExtImpl) MsgId() (msgId string) {
	msgId = m.msgId
	return
}

//SetMsgId SetMsgId
func (m *MessageExtImpl) SetMsgId(msgId string) {
	m.msgId = msgId
	return
}

//GetOriginMessageId GetOriginMessageId
func (m *MessageExtImpl) GetOriginMessageId() string {
	if m.properties != nil {
		originMessageId := m.properties[constant.PROPERTY_ORIGIN_MESSAGE_ID]
		if len(originMessageId) > 0 {
			return originMessageId
		}
	}
	return m.msgId
}

//GetConsumeStartTime GetConsumeStartTime
func (m *MessageExtImpl) GetConsumeStartTime() int64 {
	if len(m.propertyConsumeStartTimestamp) > 0 {
		return util.StrToInt64WithDefaultValue(m.propertyConsumeStartTimestamp, -1)
	}
	return math.MaxInt64
}

//SetConsumeStartTime SetConsumeStartTime
func (m *MessageExtImpl) SetConsumeStartTime() {
	if m.properties == nil {
		m.properties = make(map[string]string)
	}
	nowTime := util.CurrentTimeMillisStr()
	m.properties[constant.PROPERTY_KEYS] = nowTime
	m.propertyConsumeStartTimestamp = nowTime
	return
}
