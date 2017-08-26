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

import (
	"fmt"
)

//SendStatus message send result
type SendStatus int

const (
	//SendOK message send success
	SendOK SendStatus = iota
	//FlushDiskTimeout FlushDiskTimeout
	FlushDiskTimeout
	//FlushSlaveTimeout FlushSlaveTimeout
	FlushSlaveTimeout
	//SlaveNotAvaliable SlaveNotAvaliable
	SlaveNotAvaliable
)

//SendResult rocketmq send result
type SendResult struct {
	sendStatus    SendStatus
	msgID         string
	messageQueue  MessageQueue
	queueOffset   int64
	transactionID string
	offsetMsgID   string
	regionID      string
	traceOn       bool
}

//TraceOn TraceOn
func (result *SendResult) TraceOn() bool {
	return result.traceOn
}

//SetTraceOn SetTraceOn
func (result *SendResult) SetTraceOn(b bool) {
	result.traceOn = b
}

//SetRegionID SetRegionID
func (result *SendResult) SetRegionID(s string) {
	result.regionID = s
}

//MsgID get rocketmq message id
func (result *SendResult) MsgID() string {
	return result.msgID
}

//SetMsgID set rocketmq message id
func (result *SendResult) SetMsgID(s string) {
	result.msgID = s
}

//SendStatus SendStatus
func (result *SendResult) SendStatus() SendStatus {
	return result.sendStatus
}

//SetSendStatus SetSendStatus
func (result *SendResult) SetSendStatus(status SendStatus) {
	result.sendStatus = status
}

//MessageQueue this message send to which message queue
func (result *SendResult) MessageQueue() MessageQueue {
	return result.messageQueue
}

//SetMessageQueue SetMessageQueue
func (result *SendResult) SetMessageQueue(queue MessageQueue) {
	result.messageQueue = queue
}

//QueueOffset this message in this message queue's offset
func (result *SendResult) QueueOffset() int64 {
	return result.queueOffset
}

//SetQueueOffset SetQueueOffset
func (result *SendResult) SetQueueOffset(offset int64) {
	result.queueOffset = offset
}

//TransactionID no use,because not support transaction message
func (result *SendResult) TransactionID() string {
	return result.transactionID
}

//SetTransactionID no use,because not support transaction message
func (result *SendResult) SetTransactionID(s string) {
	result.transactionID = s
}

//OffsetMsgID OffsetMsgID
func (result *SendResult) OffsetMsgID() string {
	return result.offsetMsgID
}

//SetOffsetMsgID SetOffsetMsgID
func (result *SendResult) SetOffsetMsgID(s string) {
	result.offsetMsgID = s
}

//SendResult send message result to string(detail result)
func (result *SendResult) String() string {
	return fmt.Sprintf("SendResult [sendStatus=%d, msgId=%s, offsetMsgId=%s, messageQueue.BrokerName=%s, messageQueue.QueueId=%d, queueOffset=%d]",
		result.sendStatus, result.msgID, result.offsetMsgID, result.messageQueue.BrokerName, result.messageQueue.QueueId, result.queueOffset)
}
