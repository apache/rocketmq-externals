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
	"container/list" //TODO
	"fmt"
)

type PullStatus int

const (
	Found PullStatus = iota
	NoNewMsg
	NoMatchedMsg
	OffsetIllegal
)

type PullResult struct {
	pullStatus      PullStatus
	nextBeginOffset int64
	minOffset       int64
	maxOffset       int64
	msgFoundList    list.List
}

func NewPullResult(ps PullStatus, next, min, max int64, list list.List) *PullResult {
	return &PullResult{
		ps,
		next,
		min,
		max,
		list,
	}
}

func (result *PullResult) PullStatus() PullStatus {
	return result.pullStatus
}

func (result *PullResult) NextBeginOffset() int64 {
	return result.nextBeginOffset
}

func (result *PullResult) MaxOffset() int64 {
	return result.maxOffset
}

func (result *PullResult) MinOffset() int64 {
	return result.minOffset
}

func (result *PullResult) MsgFoundList() list.List {
	return result.msgFoundList
}

func (result *PullResult) SetMsgFoundList(list list.List) {
	result.msgFoundList = list
}

func (result *PullResult) String() string {
	return fmt.Sprintf("PullResult [pullStatus=%s, nextBeginOffset=%s, minOffset=%s, maxOffset=%s, msgFoundList=%s]",
		result.pullStatus, result.nextBeginOffset, result.minOffset, result.maxOffset, result.msgFoundList.Len()) // TODO: msgFoundList maybe nil
}
