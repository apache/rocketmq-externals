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

//func (self *ProcessQueue) TakeMessages(batchSize int) (messageToConsumeList  []MessageExt) {
//	defer self.lockTreeMap.Unlock()
//	self.lockTreeMap.Lock()
//	self.lastConsumeTimestamp = time.Now()
//	it := self.msgTreeMap.Iterator()
//	nowIndex := 0
//	for it.Next() {
//		offset, message := it.Key(), it.Value()
//		if (nowIndex >= batchSize) {
//			break
//		}
//		self.msgTreeMap.Remove(offset)
//		self.msgTreeMapToBeConsume.Put(offset, message)
//		//messageToConsumeList = append(messageToConsumeList, message)
//	}
//	if (len(messageToConsumeList) == 0) {
//		self.consuming = false
//	}
//	return
//}

/**
#
public final static long RebalanceLockMaxLiveTime =Long.parseLong(System.getProperty("rocketmq.client.rebalance.lockMaxLiveTime", "30000"));
public final static long RebalanceLockInterval = Long.parseLong(System.getProperty("rocketmq.client.rebalance.lockInterval", "20000"));
#并发消费过期的
        case CONSUME_PASSIVELY:
                            pq.setDropped(true);
                            if (this.removeUnnecessaryMessageQueue(mq, pq)) {
                                it.remove();
                                changed = true;
                                log.error("[BUG]doRebalance, {}, remove unnecessary mq, {}, because pull is pause, so try to fixed it",
                                        consumerGroup, mq);
                            }
                            break;
private final static long PullMaxIdleTime = Long.parseLong(System.getProperty("rocketmq.client.pull.pullMaxIdleTime", "120000"));
private final Logger log = ClientLogger.getLog();
private final ReadWriteLock lockTreeMap = new ReentrantReadWriteLock();

private final TreeMap<Long, MessageExt> msgTreeMap = new TreeMap<Long, MessageExt>();
private final AtomicLong msgCount = new AtomicLong();
private final Lock lockConsume = new ReentrantLock();
private final TreeMap<Long, MessageExt> msgTreeMapTemp = new TreeMap<Long, MessageExt>();
private final AtomicLong tryUnlockTimes = new AtomicLong(0);
private volatile long queueOffsetMax = 0L;
private volatile boolean dropped = false;
private volatile long lastPullTimestamp = System.currentTimeMillis();
private volatile long lastConsumeTimestamp = System.currentTimeMillis();
private volatile boolean locked = false;
private volatile long lastLockTimestamp = System.currentTimeMillis();
private volatile boolean consuming = false;
private volatile long msgAccCnt = 0;

  public boolean isLockExpired() {
        boolean result = (System.currentTimeMillis() - this.lastLockTimestamp) > RebalanceLockMaxLiveTime;
        return result;
    }


    public boolean isPullExpired() {
        boolean result = (System.currentTimeMillis() - this.lastPullTimestamp) > PullMaxIdleTime;
        return result;
    }

param pushConsumer
cleanExpiredMsg(DefaultMQPushConsumer pushConsumer) {
if (pushConsumer.getDefaultMQPushConsumerImpl().isConsumeOrderly()) {
return;
}

int loop = msgTreeMap.size() < 16 ? msgTreeMap.size() : 16;
for (int i = 0; i < loop; i++) {
MessageExt msg = null;
try {
this.lockTreeMap.readLock().lockInterruptibly();
try {
if (!msgTreeMap.isEmpty() && System.currentTimeMillis() - Long.parseLong(MessageAccessor.getConsumeStartTimeStamp(msgTreeMap.firstEntry().getValue())) > pushConsumer.getConsumeTimeout() * 60 * 1000) {
msg = msgTreeMap.firstEntry().getValue();
} else {

break;
}
} finally {
this.lockTreeMap.readLock().unlock();
}
} catch (InterruptedException e) {
log.error("getExpiredMsg exception", e);
}

try {

pushConsumer.sendMessageBack(msg, 3);
log.info("send expire msg back. topic={}, msgId={}, storeHost={}, queueId={}, queueOffset={}", msg.getTopic(), msg.getMsgId(), msg.getStoreHost(), msg.getQueueId(), msg.getQueueOffset());
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
if (!msgTreeMap.isEmpty() && msg.getQueueOffset() == msgTreeMap.firstKey()) {
try {
msgTreeMap.remove(msgTreeMap.firstKey());
} catch (Exception e) {
log.error("send expired msg exception", e);
}
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("getExpiredMsg exception", e);
}
} catch (Exception e) {
log.error("send expired msg exception", e);
}
}
}


public boolean putMessage(final List<MessageExt> msgs) {
boolean dispatchToConsume = false;
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
int validMsgCnt = 0;
for (MessageExt msg : msgs) {
MessageExt old = msgTreeMap.put(msg.getQueueOffset(), msg);
if (null == old) {
validMsgCnt++;
this.queueOffsetMax = msg.getQueueOffset();
}
}
msgCount.addAndGet(validMsgCnt);

if (!msgTreeMap.isEmpty() && !this.consuming) {
dispatchToConsume = true;
this.consuming = true;
}

if (!msgs.isEmpty()) {
MessageExt messageExt = msgs.get(msgs.size() - 1);
String property = messageExt.getProperty(MessageConst.PROPERTY_MAX_OFFSET);
if (property != null) {
long accTotal = Long.parseLong(property) - messageExt.getQueueOffset();
if (accTotal > 0) {
this.msgAccCnt = accTotal;
}
}
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("putMessage exception", e);
}

return dispatchToConsume;
}


public long getMaxSpan() {
try {
this.lockTreeMap.readLock().lockInterruptibly();
try {
if (!this.msgTreeMap.isEmpty()) {
return this.msgTreeMap.lastKey() - this.msgTreeMap.firstKey();
}
} finally {
this.lockTreeMap.readLock().unlock();
}
} catch (InterruptedException e) {
log.error("getMaxSpan exception", e);
}

return 0;
}


public long removeMessage(final List<MessageExt> msgs) { //treeMap是维护了没有消费的 为了处理过期使用
long result = -1;
final long now = System.currentTimeMillis();
try {
this.lockTreeMap.writeLock().lockInterruptibly();
this.lastConsumeTimestamp = now;
try {
if (!msgTreeMap.isEmpty()) {
result = this.queueOffsetMax + 1;
int removedCnt = 0;
for (MessageExt msg : msgs) {
MessageExt prev = msgTreeMap.remove(msg.getQueueOffset());
if (prev != null) {
removedCnt--;
}
}
msgCount.addAndGet(removedCnt);

if (!msgTreeMap.isEmpty()) {
result = msgTreeMap.firstKey();
}
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (Throwable t) {
log.error("removeMessage exception", t);
}

return result;
}


public TreeMap<Long, MessageExt> getMsgTreeMap() {
return msgTreeMap;
}


public AtomicLong getMsgCount() {
return msgCount;
}


public boolean isDropped() {
return dropped;
}


public void setDropped(boolean dropped) {
this.dropped = dropped;
}

public boolean isLocked() {
return locked;
}

public void setLocked(boolean locked) {
this.locked = locked;
}

public void rollback() {
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
this.msgTreeMap.putAll(this.msgTreeMapTemp);
this.msgTreeMapTemp.clear();
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("rollback exception", e);
}
}


public long commit() {
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
Long offset = this.msgTreeMapTemp.lastKey();
msgCount.addAndGet(this.msgTreeMapTemp.size() * (-1));
this.msgTreeMapTemp.clear();
if (offset != null) {
return offset + 1;
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("commit exception", e);
}

return -1;
}


public void makeMessageToCosumeAgain(List<MessageExt> msgs) {
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
for (MessageExt msg : msgs) {
this.msgTreeMapTemp.remove(msg.getQueueOffset());
this.msgTreeMap.put(msg.getQueueOffset(), msg);
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("makeMessageToCosumeAgain exception", e);
}
}


public List<MessageExt> takeMessags(final int batchSize) {
List<MessageExt> result = new ArrayList<MessageExt>(batchSize);
final long now = System.currentTimeMillis();
try {
this.lockTreeMap.writeLock().lockInterruptibly();
this.lastConsumeTimestamp = now;
try {
if (!this.msgTreeMap.isEmpty()) {
for (int i = 0; i < batchSize; i++) {
Map.Entry<Long, MessageExt> entry = this.msgTreeMap.pollFirstEntry();
if (entry != null) {
result.add(entry.getValue());
msgTreeMapTemp.put(entry.getKey(), entry.getValue());
} else {
break;
}
}
}

if (result.isEmpty()) {
consuming = false;
}
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("take Messages exception", e);
}

return result;
}


public boolean hasTempMessage() {
try {
this.lockTreeMap.readLock().lockInterruptibly();
try {
return !this.msgTreeMap.isEmpty();
} finally {
this.lockTreeMap.readLock().unlock();
}
} catch (InterruptedException e) {
}

return true;
}


public void clear() {
try {
this.lockTreeMap.writeLock().lockInterruptibly();
try {
this.msgTreeMap.clear();
this.msgTreeMapTemp.clear();
this.msgCount.set(0);
this.queueOffsetMax = 0L;
} finally {
this.lockTreeMap.writeLock().unlock();
}
} catch (InterruptedException e) {
log.error("rollback exception", e);
}
}




public void setLastLockTimestamp(long lastLockTimestamp) {
this.lastLockTimestamp = lastLockTimestamp;
}


public Lock getLockConsume() {
return lockConsume;
}




public void setLastPullTimestamp(long lastPullTimestamp) {
this.lastPullTimestamp = lastPullTimestamp;
}


public long getMsgAccCnt() {
return msgAccCnt;
}



public long getTryUnlockTimes() {
return this.tryUnlockTimes.get();
}


public void incTryUnlockTimes() {
this.tryUnlockTimes.incrementAndGet();
}


public void fillProcessQueueInfo(final ProcessQueueInfo info) {
try {
this.lockTreeMap.readLock().lockInterruptibly();

if (!this.msgTreeMap.isEmpty()) {
info.setCachedMsgMinOffset(this.msgTreeMap.firstKey());
info.setCachedMsgMaxOffset(this.msgTreeMap.lastKey());
info.setCachedMsgCount(this.msgTreeMap.size());
}

if (!this.msgTreeMapTemp.isEmpty()) {
info.setTransactionMsgMinOffset(this.msgTreeMapTemp.firstKey());
info.setTransactionMsgMaxOffset(this.msgTreeMapTemp.lastKey());
info.setTransactionMsgCount(this.msgTreeMapTemp.size());
}

info.setLocked(this.locked);
info.setTryUnlockTimes(this.tryUnlockTimes.get());
info.setLastLockTimestamp(this.lastLockTimestamp);

info.setDroped(this.dropped);
info.setLastPullTimestamp(this.lastPullTimestamp);
info.setLastConsumeTimestamp(this.lastConsumeTimestamp);
} catch (Exception e) {
} finally {
this.lockTreeMap.readLock().unlock();
}
}



*/
