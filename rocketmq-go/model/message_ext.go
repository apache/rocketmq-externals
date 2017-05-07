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
	"math"
	"strconv"
	"time"
)

type MessageExt struct {
	*Message
	QueueId                   int32
	StoreSize                 int32
	QueueOffset               int64
	SysFlag                   int32
	BornTimestamp             int64
	BornHost                  string
	StoreTimestamp            int64
	StoreHost                 string
	MsgId                     string
	CommitLogOffset           int64
	BodyCRC                   int32
	ReconsumeTimes            int32
	PreparedTransactionOffset int64

	propertyConsumeStartTimestamp string // race condition
}

func (self *MessageExt) GetOriginMessageId() string {
	if self.Properties != nil {
		originMessageId := self.Properties[constant.PROPERTY_ORIGIN_MESSAGE_ID]
		if len(originMessageId) > 0 {
			return originMessageId
		}
	}
	return self.MsgId
}

func (self *MessageExt) GetConsumeStartTime() int64 {
	if len(self.propertyConsumeStartTimestamp) > 0 {
		return util.StrToInt64WithDefaultValue(self.propertyConsumeStartTimestamp, -1)
	}
	return math.MaxInt64
}

func (self *MessageExt) SetConsumeStartTime() {
	if self.Properties == nil {
		self.Properties = make(map[string]string)
	}
	nowTime := strconv.FormatInt(time.Now().UnixNano()/1000000, 10)
	self.Properties[constant.PROPERTY_KEYS] = nowTime
	self.propertyConsumeStartTimestamp = nowTime
	return
}
