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
	"github.com/emirpasic/gods/maps/treemap"
	"github.com/golang/glog"
	"sync"
	"time"
)

type ProcessQueue struct {
	msgTreeMap            *treemap.Map // int | MessageExt
	msgCount              int
	lockTreeMap           sync.RWMutex
	locked                bool
	lastPullTimestamp     time.Time
	lastConsumeTimestamp  time.Time
	lastLockTimestamp     time.Time
	lockConsume           sync.RWMutex
	consuming             bool
	queueOffsetMax        int64
	dropped               bool
	msgAccCnt             int64 //accumulation message count
	tryUnlockTimes        int64
	msgTreeMapToBeConsume *treemap.Map
}

func NewProcessQueue() (processQueue *ProcessQueue) {
	processQueue = new(ProcessQueue)
	processQueue.dropped = false
	processQueue.msgTreeMap = treemap.NewWithIntComparator()
	processQueue.msgTreeMapToBeConsume = treemap.NewWithIntComparator()

	return
}
func (self *ProcessQueue) GetMsgCount() int {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	return self.msgCount
}

func (self *ProcessQueue) Clear() {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	self.SetDrop(true)
	self.msgTreeMap.Clear()
	self.msgCount = 0
	self.queueOffsetMax = 0

}

func (self *ProcessQueue) ChangeToProcessQueueInfo() (processQueueInfo ProcessQueueInfo) {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	processQueueInfo = ProcessQueueInfo{}
	minOffset := -1
	maxOffset := -1
	minKey, _ := self.msgTreeMap.Min()
	if minKey != nil {
		minOffset = minKey.(int)
	}
	maxKey, _ := self.msgTreeMap.Max()
	if maxKey != nil {
		maxOffset = maxKey.(int)
	}
	processQueueInfo.CachedMsgCount = int32(self.msgCount)
	processQueueInfo.CachedMsgMinOffset = int64(maxOffset)
	processQueueInfo.CachedMsgMaxOffset = int64(minOffset)
	//processQueueInfo.CommitOffset = -123 // todo
	processQueueInfo.Droped = self.dropped
	processQueueInfo.LastConsumeTimestamp = self.lastConsumeTimestamp.UnixNano()
	processQueueInfo.LastPullTimestamp = self.lastPullTimestamp.UnixNano()
	//processQueueInfo.

	return
}

func (self *ProcessQueue) DeleteExpireMsg(queueOffset int) {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	key, _ := self.msgTreeMap.Min()
	if key == nil {
		return
	}
	offset := key.(int)
	glog.Infof("look min key and offset  %d  %s", offset, queueOffset)
	if queueOffset == offset {
		self.msgTreeMap.Remove(queueOffset)
		self.msgCount = self.msgTreeMap.Size()
	}
}

func (self *ProcessQueue) GetMinMessageInTree() (offset int, messagePoint *MessageExt) {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	key, value := self.msgTreeMap.Min()
	if key == nil || value == nil {
		return
	}
	offset = key.(int)

	message := value.(MessageExt)
	messagePoint = &message
	return
}

func (self *ProcessQueue) SetDrop(drop bool) {
	self.dropped = drop
}
func (self *ProcessQueue) IsDropped() bool {
	return self.dropped
}
func (self *ProcessQueue) GetMaxSpan() int {
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	if self.msgTreeMap.Empty() {
		return 0
	}
	minKey, _ := self.msgTreeMap.Min()
	minOffset := minKey.(int)
	maxKey, _ := self.msgTreeMap.Max()
	maxOffset := maxKey.(int)
	return maxOffset - minOffset
}

func (self *ProcessQueue) RemoveMessage(msgs []MessageExt) (offset int64) {
	now := time.Now()
	offset = -1
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()
	self.lastConsumeTimestamp = now
	if self.msgCount > 0 {
		maxKey, _ := self.msgTreeMap.Max()
		offset = int64(maxKey.(int)) + 1
		for _, msg := range msgs {
			self.msgTreeMap.Remove(int(msg.QueueOffset))
		}
		self.msgCount = self.msgTreeMap.Size()
		if self.msgCount > 0 {
			minKey, _ := self.msgTreeMap.Min()
			offset = int64(minKey.(int))
		}
	}
	return
}

func (self *ProcessQueue) PutMessage(msgs []MessageExt) (dispatchToConsume bool) {
	dispatchToConsume = false
	msgsLen := len(msgs)
	if msgsLen == 0 {
		return
	}
	defer self.lockTreeMap.Unlock()
	self.lockTreeMap.Lock()

	for _, msg := range msgs {
		self.msgTreeMap.Put(int(msg.QueueOffset), msg)

	}
	self.msgCount = self.msgTreeMap.Size()
	maxOffset, _ := self.msgTreeMap.Max()
	self.queueOffsetMax = int64(maxOffset.(int))
	if self.msgCount > 0 && !self.consuming {
		dispatchToConsume = true
		self.consuming = true
	}
	lastMsg := msgs[msgsLen-1]
	remoteMaxOffset := util.StrToInt64WithDefaultValue(lastMsg.Properties[constant.PROPERTY_MAX_OFFSET], -1)
	if remoteMaxOffset > 0 {
		accTotal := remoteMaxOffset - lastMsg.QueueOffset
		if accTotal > 0 {
			self.msgAccCnt = accTotal
		}
	}
	return
}
