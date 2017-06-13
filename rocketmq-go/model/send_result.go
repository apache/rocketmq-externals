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

type SendStatus int

const (
	SendOK SendStatus = iota
	//FlushDiskTimeout
	//FlushSlaveTimeout
	SlaveNotAvaliable
)

type SendResult struct {
	sendStatus    SendStatus
	msgID         string
	messageQueue  *message.MessageQueue
	queueOffset   int64
	transactionID string
	offsetMsgID   string
	regionID      string
	traceOn       bool
}

func NewSendResult(status SendStatus, msgID, offsetID string, queue *message.MessageQueue, queueOffset int64) *SendResult {
	return &SendResult{
		sendStatus:   status,
		msgID:        msgID,
		offsetMsgID:  offsetID,
		messageQueue: queue,
		queueOffset:  queueOffset,
	}
}

func EncoderSendResultToJson(obj interface{}) string {
	return "" // TODO
}

func DecoderSendResultFromJson(json string) *SendResult {
	return nil // TODO
}

func (result *SendResult) TraceOn() bool {
	return result.traceOn
}

func (result *SendResult) SetTraceOn(b bool) {
	result.traceOn = b
}

func (result *SendResult) SetRegionID(s string) {
	result.regionID = s
}

func (result *SendResult) MsgID() string {
	return result.msgID
}

func (result *SendResult) SetMsgID(s string) {
	result.msgID = s
}

func (result *SendResult) SendStatus() SendStatus {
	return result.sendStatus
}

func (result *SendResult) SetSendStatus(status SendStatus) {
	result.sendStatus = status
}

func (result *SendResult) MessageQueue() *message.MessageQueue {
	return result.messageQueue
}

func (result *SendResult) SetMessageQueue(queue *message.MessageQueue) {
	result.messageQueue = queue
}

func (result *SendResult) QueueOffset() int64 {
	return result.queueOffset
}

func (result *SendResult) SetQueueOffset(offset int64) {
	result.queueOffset = offset
}

func (result *SendResult) TransactionID() string {
	return result.transactionID
}

func (result *SendResult) SetTransactionID(s string) {
	result.transactionID = s
}

func (result *SendResult) OffsetMsgID() string {
	return result.offsetMsgID
}

func (result *SendResult) SetOffsetMsgID(s string) {
	result.offsetMsgID = s
}

func (result *SendResult) String() string {
	return fmt.Sprintf("SendResult [sendStatus=%s, msgId=%s, offsetMsgId=%s, messageQueue=%s, queueOffset=%s]",
		result.sendStatus, result.msgID, result.offsetMsgID, result.messageQueue, result.queueOffset)
}
