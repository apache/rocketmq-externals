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
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
)

type QueryResult struct {
	indexLastUpdateTimestamp int64
	messageList              []*message.MessageExt
}

func NewQueryResult(timestamp int64, list []*message.MessageExt) *QueryResult {
	return &QueryResult{
		indexLastUpdateTimestamp: timestamp,
		messageList:              list,
	}
}

func (qr *QueryResult) IndexLastUpdateTimestamp() int64 {
	return qr.indexLastUpdateTimestamp
}

func (qr *QueryResult) MessageList() []*message.MessageExt { //TODO: address?
	return qr.messageList
}

func (qr *QueryResult) String() string {
	return fmt.Sprintf("QueryResult [indexLastUpdateTimestamp=%s, messageList=%s]",
		qr.indexLastUpdateTimestamp, qr.messageList)
}
