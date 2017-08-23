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

package model

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/emirpasic/gods/maps/treemap"
	"github.com/golang/glog"
	"sync"
	"time"
)

//ProcessQueue message process queue
type ProcessQueue struct {
	msgTreeMap            *treemap.Map // int | MessageExtImpl
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

//NewProcessQueue create a ProcessQueue
func NewProcessQueue() (processQueue *ProcessQueue) {
	processQueue = new(ProcessQueue)
	processQueue.dropped = false
	processQueue.msgTreeMap = treemap.NewWithIntComparator()
	processQueue.msgTreeMapToBeConsume = treemap.NewWithIntComparator()

	return
}

//GetMsgCount get message count
func (p *ProcessQueue) GetMsgCount() int {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	return p.msgCount
}

//Clear clear
func (p *ProcessQueue) Clear() {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	p.SetDrop(true)
	p.msgTreeMap.Clear()
	p.msgCount = 0
	p.queueOffsetMax = 0

}

//ChangeToProcessQueueInfo changeToProcessQueueInfo
func (p *ProcessQueue) ChangeToProcessQueueInfo() (processQueueInfo ProcessQueueInfo) {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	processQueueInfo = ProcessQueueInfo{}
	minOffset := -1
	maxOffset := -1
	minKey, _ := p.msgTreeMap.Min()
	if minKey != nil {
		minOffset = minKey.(int)
	}
	maxKey, _ := p.msgTreeMap.Max()
	if maxKey != nil {
		maxOffset = maxKey.(int)
	}
	processQueueInfo.CachedMsgCount = int32(p.msgCount)
	processQueueInfo.CachedMsgMinOffset = int64(maxOffset)
	processQueueInfo.CachedMsgMaxOffset = int64(minOffset)
	processQueueInfo.Droped = p.dropped
	processQueueInfo.LastConsumeTimestamp = p.lastConsumeTimestamp.UnixNano()
	processQueueInfo.LastPullTimestamp = p.lastPullTimestamp.UnixNano()
	return
}

//DeleteExpireMsg deleteExpireMsg
func (p *ProcessQueue) DeleteExpireMsg(queueOffset int) {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	key, _ := p.msgTreeMap.Min()
	if key == nil {
		return
	}
	offset := key.(int)
	glog.V(2).Infof("look min key and offset  %d  %s", offset, queueOffset)
	if queueOffset == offset {
		p.msgTreeMap.Remove(queueOffset)
		p.msgCount = p.msgTreeMap.Size()
	}
}

//GetMinMessageInTree getMinMessageInTree
func (p *ProcessQueue) GetMinMessageInTree() (offset int, messagePoint *message.MessageExtImpl) {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	key, value := p.msgTreeMap.Min()
	if key == nil || value == nil {
		return
	}
	offset = key.(int)

	message := value.(message.MessageExtImpl)
	messagePoint = &message
	return
}

//SetDrop set this queue is dropped
func (p *ProcessQueue) SetDrop(drop bool) {
	p.dropped = drop
}

//IsDropped judge whether this queue is dropped
func (p *ProcessQueue) IsDropped() bool {
	return p.dropped
}

//GetMaxSpan getMaxSpan
func (p *ProcessQueue) GetMaxSpan() int {
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	if p.msgTreeMap.Empty() {
		return 0
	}
	minKey, _ := p.msgTreeMap.Min()
	minOffset := minKey.(int)
	maxKey, _ := p.msgTreeMap.Max()
	maxOffset := maxKey.(int)
	return maxOffset - minOffset
}

//RemoveMessage from this process queue
func (p *ProcessQueue) RemoveMessage(msgs []message.MessageExtImpl) (offset int64) {
	now := time.Now()
	offset = -1
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()
	p.lastConsumeTimestamp = now
	if p.msgCount > 0 {
		maxKey, _ := p.msgTreeMap.Max()
		offset = int64(maxKey.(int)) + 1
		for _, msg := range msgs {
			p.msgTreeMap.Remove(int(msg.QueueOffset))
		}
		p.msgCount = p.msgTreeMap.Size()
		if p.msgCount > 0 {
			minKey, _ := p.msgTreeMap.Min()
			offset = int64(minKey.(int))
		}
	}
	return
}

//PutMessage put message into this process queue
func (p *ProcessQueue) PutMessage(msgs []message.MessageExtImpl) (dispatchToConsume bool) {
	dispatchToConsume = false
	msgsLen := len(msgs)
	if msgsLen == 0 {
		return
	}
	defer p.lockTreeMap.Unlock()
	p.lockTreeMap.Lock()

	for _, msg := range msgs {
		p.msgTreeMap.Put(int(msg.QueueOffset), msg)

	}
	p.msgCount = p.msgTreeMap.Size()
	maxOffset, _ := p.msgTreeMap.Max()
	p.queueOffsetMax = int64(maxOffset.(int))
	if p.msgCount > 0 && !p.consuming {
		dispatchToConsume = true
		p.consuming = true
	}
	lastMsg := msgs[msgsLen-1]
	remoteMaxOffset := util.StrToInt64WithDefaultValue(lastMsg.PropertiesKeyValue(constant.PROPERTY_MAX_OFFSET), -1)
	if remoteMaxOffset > 0 {
		accTotal := remoteMaxOffset - lastMsg.QueueOffset
		if accTotal > 0 {
			p.msgAccCnt = accTotal
		}
	}
	return
}
