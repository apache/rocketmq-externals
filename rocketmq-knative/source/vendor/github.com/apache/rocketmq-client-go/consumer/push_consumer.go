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

package consumer

import (
	"context"
	"fmt"
	"math"
	"strconv"
	"sync"
	"sync/atomic"
	"time"

	"github.com/pkg/errors"

	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/internal/utils"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
)

// In most scenarios, this is the mostly recommended usage to consume messages.
//
// Technically speaking, this push client is virtually a wrapper of the underlying pull service. Specifically, on
// arrival of messages pulled from brokers, it roughly invokes the registered callback handler to feed the messages.
//
// See quick start/Consumer in the example module for a typical usage.
//
// <strong>Thread Safety:</strong> After initialization, the instance can be regarded as thread-safe.

const (
	Mb = 1024 * 1024
)

type PushConsumerCallback struct {
	topic string
	f     func(context.Context, ...*primitive.MessageExt) (ConsumeResult, error)
}

func (callback PushConsumerCallback) UniqueID() string {
	return callback.topic
}

type pushConsumer struct {
	*defaultConsumer
	queueFlowControlTimes        int
	queueMaxSpanFlowControlTimes int
	consumeFunc                  utils.Set
	submitToConsume              func(*processQueue, *primitive.MessageQueue)
	subscribedTopic              map[string]string
	interceptor                  primitive.Interceptor
	queueLock                    *QueueLock
	done                         chan struct{}
	closeOnce                    sync.Once
}

func NewPushConsumer(opts ...Option) (*pushConsumer, error) {
	defaultOpts := defaultPushConsumerOptions()
	for _, apply := range opts {
		apply(&defaultOpts)
	}
	srvs, err := internal.NewNamesrv(defaultOpts.NameServerAddrs)
	if err != nil {
		return nil, errors.Wrap(err, "new Namesrv failed.")
	}
	if !defaultOpts.Credentials.IsEmpty() {
		srvs.SetCredentials(defaultOpts.Credentials)
	}
	defaultOpts.Namesrv = srvs

	if defaultOpts.Namespace != "" {
		defaultOpts.GroupName = defaultOpts.Namespace + "%" + defaultOpts.GroupName
	}

	dc := &defaultConsumer{
		client:         internal.GetOrNewRocketMQClient(defaultOpts.ClientOptions, nil),
		consumerGroup:  defaultOpts.GroupName,
		cType:          _PushConsume,
		state:          int32(internal.StateCreateJust),
		prCh:           make(chan PullRequest, 4),
		model:          defaultOpts.ConsumerModel,
		consumeOrderly: defaultOpts.ConsumeOrderly,
		fromWhere:      defaultOpts.FromWhere,
		allocate:       defaultOpts.Strategy,
		option:         defaultOpts,
		namesrv:        srvs,
	}

	p := &pushConsumer{
		defaultConsumer: dc,
		subscribedTopic: make(map[string]string, 0),
		queueLock:       newQueueLock(),
		done:            make(chan struct{}, 1),
		consumeFunc:     utils.NewSet(),
	}
	dc.mqChanged = p.messageQueueChanged
	if p.consumeOrderly {
		p.submitToConsume = p.consumeMessageOrderly
	} else {
		p.submitToConsume = p.consumeMessageCurrently
	}

	p.interceptor = primitive.ChainInterceptors(p.option.Interceptors...)

	return p, nil
}

func (pc *pushConsumer) Start() error {
	var err error
	pc.once.Do(func() {
		rlog.Info("the consumer start beginning", map[string]interface{}{
			rlog.LogKeyConsumerGroup: pc.consumerGroup,
			"messageModel":           pc.model,
			"unitMode":               pc.unitMode,
		})
		atomic.StoreInt32(&pc.state, int32(internal.StateStartFailed))
		pc.validate()

		err = pc.client.RegisterConsumer(pc.consumerGroup, pc)
		if err != nil {
			rlog.Error("the consumer group has been created, specify another one", map[string]interface{}{
				rlog.LogKeyConsumerGroup: pc.consumerGroup,
			})
			err = ErrCreated
			return
		}

		err = pc.defaultConsumer.start()
		if err != nil {
			return
		}

		go func() {
			// todo start clean msg expired
			for {
				select {
				case pr := <-pc.prCh:
					go func() {
						pc.pullMessage(&pr)
					}()
				case <-pc.done:
					rlog.Info("push consumer close pullConsumer listener.", map[string]interface{}{
						rlog.LogKeyConsumerGroup: pc.consumerGroup,
					})
					return
				}
			}
		}()

		pc.Rebalance()
		time.Sleep(1 * time.Second)

		go primitive.WithRecover(func() {
			// initial lock.
			time.Sleep(1000 * time.Millisecond)
			pc.lockAll()

			lockTicker := time.NewTicker(pc.option.RebalanceLockInterval)
			defer lockTicker.Stop()
			for {
				select {
				case <-lockTicker.C:
					pc.lockAll()
				case <-pc.done:
					rlog.Info("push consumer close tick.", map[string]interface{}{
						rlog.LogKeyConsumerGroup: pc.consumerGroup,
					})
					return
				}
			}
		})
	})

	if err != nil {
		return err
	}

	pc.client.UpdateTopicRouteInfo()
	for k := range pc.subscribedTopic {
		_, exist := pc.topicSubscribeInfoTable.Load(k)
		if !exist {
			pc.client.Shutdown()
			return fmt.Errorf("the topic=%s route info not found, it may not exist", k)
		}
	}
	pc.client.RebalanceImmediately()
	pc.client.CheckClientInBroker()
	pc.client.SendHeartbeatToAllBrokerWithLock()

	return err
}

func (pc *pushConsumer) Shutdown() error {
	var err error
	pc.closeOnce.Do(func() {
		close(pc.done)

		pc.client.UnregisterConsumer(pc.consumerGroup)
		err = pc.defaultConsumer.shutdown()
	})

	return err
}

func (pc *pushConsumer) Subscribe(topic string, selector MessageSelector,
	f func(context.Context, ...*primitive.MessageExt) (ConsumeResult, error)) error {
	if atomic.LoadInt32(&pc.state) != int32(internal.StateCreateJust) {
		return errors.New("subscribe topic only started before")
	}
	if pc.option.Namespace != "" {
		topic = pc.option.Namespace + "%" + topic
	}
	data := buildSubscriptionData(topic, selector)
	pc.subscriptionDataTable.Store(topic, data)
	pc.subscribedTopic[topic] = ""

	if pc.option.ConsumerModel == Clustering {
		// add retry topic for clustering mode
		retryTopic := internal.GetRetryTopic(pc.consumerGroup)
		retryData := buildSubscriptionData(retryTopic, MessageSelector{Expression: _SubAll})
		pc.subscriptionDataTable.Store(retryTopic, retryData)
		pc.subscribedTopic[retryTopic] = ""
	}

	pc.consumeFunc.Add(&PushConsumerCallback{
		f:     f,
		topic: topic,
	})
	return nil
}

func (pc *pushConsumer) Unsubscribe(string) error {
	return nil
}

func (pc *pushConsumer) Rebalance() {
	pc.defaultConsumer.doBalance()
}

func (pc *pushConsumer) PersistConsumerOffset() error {
	return pc.defaultConsumer.persistConsumerOffset()
}

func (pc *pushConsumer) UpdateTopicSubscribeInfo(topic string, mqs []*primitive.MessageQueue) {
	pc.defaultConsumer.updateTopicSubscribeInfo(topic, mqs)
}

func (pc *pushConsumer) IsSubscribeTopicNeedUpdate(topic string) bool {
	return pc.defaultConsumer.isSubscribeTopicNeedUpdate(topic)
}

func (pc *pushConsumer) SubscriptionDataList() []*internal.SubscriptionData {
	return pc.defaultConsumer.SubscriptionDataList()
}

func (pc *pushConsumer) IsUnitMode() bool {
	return pc.unitMode
}

func (pc *pushConsumer) GetConsumerRunningInfo() *internal.ConsumerRunningInfo {
	info := internal.NewConsumerRunningInfo()

	pc.subscriptionDataTable.Range(func(key, value interface{}) bool {
		topic := key.(string)
		info.SubscriptionData[value.(*internal.SubscriptionData)] = true
		status := internal.ConsumeStatus{
			PullRT:            getPullRT(topic, pc.consumerGroup).avgpt,
			PullTPS:           getPullTPS(topic, pc.consumerGroup).tps,
			ConsumeRT:         getConsumeRT(topic, pc.consumerGroup).avgpt,
			ConsumeOKTPS:      getConsumeOKTPS(topic, pc.consumerGroup).tps,
			ConsumeFailedTPS:  getConsumeFailedTPS(topic, pc.consumerGroup).tps,
			ConsumeFailedMsgs: topicAndGroupConsumeFailedTPS.getStatsDataInHour(topic + "@" + pc.consumerGroup).sum,
		}
		info.StatusTable[topic] = status
		return true
	})

	pc.processQueueTable.Range(func(key, value interface{}) bool {
		mq := key.(primitive.MessageQueue)
		pq := value.(*processQueue)
		pInfo := pq.currentInfo()
		pInfo.CommitOffset = pc.storage.read(&mq, _ReadMemoryThenStore)
		info.MQTable[mq] = pInfo
		return true
	})

	nsAddr := ""
	for _, value := range pc.namesrv.AddrList() {
		nsAddr += fmt.Sprintf("%s;", value)
	}
	info.Properties[internal.PropNameServerAddr] = nsAddr
	info.Properties[internal.PropConsumeType] = string(pc.cType)
	info.Properties[internal.PropConsumeOrderly] = strconv.FormatBool(pc.consumeOrderly)
	info.Properties[internal.PropThreadPoolCoreSize] = "-1"
	info.Properties[internal.PropConsumerStartTimestamp] = strconv.FormatInt(pc.consumerStartTimestamp, 10)
	return info
}

func (pc *pushConsumer) messageQueueChanged(topic string, mqAll, mqDivided []*primitive.MessageQueue) {
	v, exit := pc.subscriptionDataTable.Load(topic)
	if !exit {
		return
	}
	data := v.(*internal.SubscriptionData)
	newVersion := time.Now().UnixNano()
	rlog.Info("the MessageQueue changed, version also updated", map[string]interface{}{
		rlog.LogKeyValueChangedFrom: data.SubVersion,
		rlog.LogKeyValueChangedTo:   newVersion,
	})
	data.SubVersion = newVersion

	// TODO: optimize
	count := 0
	pc.processQueueTable.Range(func(key, value interface{}) bool {
		count++
		return true
	})
	if count > 0 {
		if pc.option.PullThresholdForTopic != -1 {
			newVal := pc.option.PullThresholdForTopic / count
			if newVal == 0 {
				newVal = 1
			}
			rlog.Info("The PullThresholdForTopic is changed", map[string]interface{}{
				rlog.LogKeyValueChangedFrom: pc.option.PullThresholdForTopic,
				rlog.LogKeyValueChangedTo:   newVal,
			})
			pc.option.PullThresholdForTopic = newVal
		}

		if pc.option.PullThresholdSizeForTopic != -1 {
			newVal := pc.option.PullThresholdSizeForTopic / count
			if newVal == 0 {
				newVal = 1
			}
			rlog.Info("The PullThresholdSizeForTopic is changed", map[string]interface{}{
				rlog.LogKeyValueChangedFrom: pc.option.PullThresholdSizeForTopic,
				rlog.LogKeyValueChangedTo:   newVal,
			})
		}
	}
	pc.client.SendHeartbeatToAllBrokerWithLock()
}

func (pc *pushConsumer) validate() {
	internal.ValidateGroup(pc.consumerGroup)

	if pc.consumerGroup == internal.DefaultConsumerGroup {
		// TODO FQA
		rlog.Error(fmt.Sprintf("consumerGroup can't equal [%s], please specify another one.", internal.DefaultConsumerGroup), nil)
	}

	if len(pc.subscribedTopic) == 0 {
		rlog.Error("number of subscribed topics is 0.", nil)
	}

	if pc.option.ConsumeConcurrentlyMaxSpan < 1 || pc.option.ConsumeConcurrentlyMaxSpan > 65535 {
		if pc.option.ConsumeConcurrentlyMaxSpan == 0 {
			pc.option.ConsumeConcurrentlyMaxSpan = 1000
		} else {
			rlog.Error("option.ConsumeConcurrentlyMaxSpan out of range [1, 65535]", nil)
		}
	}

	if pc.option.PullThresholdForQueue < 1 || pc.option.PullThresholdForQueue > 65535 {
		if pc.option.PullThresholdForQueue == 0 {
			pc.option.PullThresholdForQueue = 1024
		} else {
			rlog.Error("option.PullThresholdForQueue out of range [1, 65535]", nil)
		}
	}

	if pc.option.PullThresholdForTopic < 1 || pc.option.PullThresholdForTopic > 6553500 {
		if pc.option.PullThresholdForTopic == 0 {
			pc.option.PullThresholdForTopic = 102400
		} else {
			rlog.Error("option.PullThresholdForTopic out of range [1, 6553500]", nil)
		}
	}

	if pc.option.PullThresholdSizeForQueue < 1 || pc.option.PullThresholdSizeForQueue > 1024 {
		if pc.option.PullThresholdSizeForQueue == 0 {
			pc.option.PullThresholdSizeForQueue = 512
		} else {
			rlog.Error("option.PullThresholdSizeForQueue out of range [1, 1024]", nil)
		}
	}

	if pc.option.PullThresholdSizeForTopic < 1 || pc.option.PullThresholdSizeForTopic > 102400 {
		if pc.option.PullThresholdSizeForTopic == 0 {
			pc.option.PullThresholdSizeForTopic = 51200
		} else {
			rlog.Error("option.PullThresholdSizeForTopic out of range [1, 102400]", nil)
		}
	}

	if pc.option.PullInterval < 0 || pc.option.PullInterval > 65535 {
		rlog.Error("option.PullInterval out of range [0, 65535]", nil)
	}

	if pc.option.ConsumeMessageBatchMaxSize < 1 || pc.option.ConsumeMessageBatchMaxSize > 1024 {
		if pc.option.ConsumeMessageBatchMaxSize == 0 {
			pc.option.ConsumeMessageBatchMaxSize = 512
		} else {
			rlog.Error("option.ConsumeMessageBatchMaxSize out of range [1, 1024]", nil)
		}
	}

	if pc.option.PullBatchSize < 1 || pc.option.PullBatchSize > 1024 {
		if pc.option.PullBatchSize == 0 {
			pc.option.PullBatchSize = 32
		} else {
			rlog.Error("option.PullBatchSize out of range [1, 1024]", nil)
		}
	}
}

func (pc *pushConsumer) pullMessage(request *PullRequest) {
	rlog.Debug("start a new Pull Message task for PullRequest", map[string]interface{}{
		rlog.LogKeyPullRequest: request.String(),
	})
	var sleepTime time.Duration
	pq := request.pq
	go primitive.WithRecover(func() {
		for {
			select {
			case <-pc.done:
				rlog.Info("push consumer close pullMessage.", map[string]interface{}{
					rlog.LogKeyConsumerGroup: pc.consumerGroup,
				})
				return
			default:
				pc.submitToConsume(request.pq, request.mq)
			}
		}
	})

	for {
	NEXT:
		select {
		case <-pc.done:
			rlog.Info("push consumer close message handle.", map[string]interface{}{
				rlog.LogKeyConsumerGroup: pc.consumerGroup,
			})
			return
		default:
		}

		if pq.IsDroppd() {
			rlog.Debug("the request was dropped, so stop task", map[string]interface{}{
				rlog.LogKeyPullRequest: request.String(),
			})
			return
		}
		if sleepTime > 0 {
			rlog.Debug(fmt.Sprintf("pull MessageQueue: %d sleep %d ms for mq: %v", request.mq.QueueId, sleepTime/time.Millisecond, request.mq), nil)
			time.Sleep(sleepTime)
		}
		// reset time
		sleepTime = pc.option.PullInterval
		pq.lastPullTime = time.Now()
		err := pc.makeSureStateOK()
		if err != nil {
			rlog.Warning("consumer state error", map[string]interface{}{
				rlog.LogKeyUnderlayError: err.Error(),
			})
			sleepTime = _PullDelayTimeWhenError
			goto NEXT
		}

		if pc.pause {
			rlog.Debug(fmt.Sprintf("consumer [%s] of [%s] was paused, execute pull request [%s] later",
				pc.option.InstanceName, pc.consumerGroup, request.String()), nil)
			sleepTime = _PullDelayTimeWhenSuspend
			goto NEXT
		}

		cachedMessageSizeInMiB := int(pq.cachedMsgSize / Mb)
		if pq.cachedMsgCount > pc.option.PullThresholdForQueue {
			if pc.queueFlowControlTimes%1000 == 0 {
				rlog.Warning("the cached message count exceeds the threshold, so do flow control", map[string]interface{}{
					"PullThresholdForQueue": pc.option.PullThresholdForQueue,
					"minOffset":             pq.Min(),
					"maxOffset":             pq.Max(),
					"count":                 pq.msgCache,
					"size(MiB)":             cachedMessageSizeInMiB,
					"flowControlTimes":      pc.queueFlowControlTimes,
					rlog.LogKeyPullRequest:  request.String(),
				})
			}
			pc.queueFlowControlTimes++
			sleepTime = _PullDelayTimeWhenFlowControl
			goto NEXT
		}

		if cachedMessageSizeInMiB > pc.option.PullThresholdSizeForQueue {
			if pc.queueFlowControlTimes%1000 == 0 {
				rlog.Warning("the cached message size exceeds the threshold, so do flow control", map[string]interface{}{
					"PullThresholdSizeForQueue": pc.option.PullThresholdSizeForQueue,
					"minOffset":                 pq.Min(),
					"maxOffset":                 pq.Max(),
					"count":                     pq.msgCache,
					"size(MiB)":                 cachedMessageSizeInMiB,
					"flowControlTimes":          pc.queueFlowControlTimes,
					rlog.LogKeyPullRequest:      request.String(),
				})
			}
			pc.queueFlowControlTimes++
			sleepTime = _PullDelayTimeWhenFlowControl
			goto NEXT
		}

		if !pc.consumeOrderly {
			if pq.getMaxSpan() > pc.option.ConsumeConcurrentlyMaxSpan {
				if pc.queueMaxSpanFlowControlTimes%1000 == 0 {
					rlog.Warning("the queue's messages span too long, so do flow control", map[string]interface{}{
						"ConsumeConcurrentlyMaxSpan": pc.option.ConsumeConcurrentlyMaxSpan,
						"minOffset":                  pq.Min(),
						"maxOffset":                  pq.Max(),
						"maxSpan":                    pq.getMaxSpan(),
						"flowControlTimes":           pc.queueFlowControlTimes,
						rlog.LogKeyPullRequest:       request.String(),
					})
				}
				sleepTime = _PullDelayTimeWhenFlowControl
				goto NEXT
			}
		} else {
			if pq.IsLock() {
				if !request.lockedFirst {
					offset := pc.computePullFromWhere(request.mq)
					brokerBusy := offset < request.nextOffset
					rlog.Info("the first time to pull message, so fix offset from broker, offset maybe changed", map[string]interface{}{
						rlog.LogKeyPullRequest:      request.String(),
						rlog.LogKeyValueChangedFrom: request.nextOffset,
						rlog.LogKeyValueChangedTo:   offset,
						"brokerBusy":                brokerBusy,
					})
					if brokerBusy {
						rlog.Info("[NOTIFY_ME] the first time to pull message, but pull request offset larger than "+
							"broker consume offset", map[string]interface{}{"offset": offset})
					}
					request.lockedFirst = true
					request.nextOffset = offset
				}
			} else {
				rlog.Info("pull message later because not locked in broker", map[string]interface{}{
					rlog.LogKeyPullRequest: request.String(),
				})
				sleepTime = _PullDelayTimeWhenError
				goto NEXT
			}
		}

		v, exist := pc.subscriptionDataTable.Load(request.mq.Topic)
		if !exist {
			rlog.Info("find the consumer's subscription failed", map[string]interface{}{
				rlog.LogKeyPullRequest: request.String(),
			})
			sleepTime = _PullDelayTimeWhenError
			goto NEXT
		}
		beginTime := time.Now()
		var (
			commitOffsetEnable bool
			commitOffsetValue  int64
			subExpression      string
		)

		if pc.model == Clustering {
			commitOffsetValue = pc.storage.read(request.mq, _ReadFromMemory)
			if commitOffsetValue > 0 {
				commitOffsetEnable = true
			}
		}

		sd := v.(*internal.SubscriptionData)
		classFilter := sd.ClassFilterMode
		if pc.option.PostSubscriptionWhenPull && classFilter {
			subExpression = sd.SubString
		}

		sysFlag := buildSysFlag(commitOffsetEnable, true, subExpression != "", classFilter)

		pullRequest := &internal.PullMessageRequestHeader{
			ConsumerGroup:        pc.consumerGroup,
			Topic:                request.mq.Topic,
			QueueId:              int32(request.mq.QueueId),
			QueueOffset:          request.nextOffset,
			MaxMsgNums:           pc.option.PullBatchSize,
			SysFlag:              sysFlag,
			CommitOffset:         commitOffsetValue,
			SubExpression:        _SubAll,
			ExpressionType:       string(TAG),
			SuspendTimeoutMillis: 20 * time.Second,
		}
		//
		//if data.ExpType == string(TAG) {
		//	pullRequest.SubVersion = 0
		//} else {
		//	pullRequest.SubVersion = data.SubVersion
		//}

		brokerResult := pc.defaultConsumer.tryFindBroker(request.mq)
		if brokerResult == nil {
			rlog.Warning("no broker found for mq", map[string]interface{}{
				rlog.LogKeyPullRequest: request.mq.String(),
			})
			sleepTime = _PullDelayTimeWhenError
			goto NEXT
		}

		result, err := pc.client.PullMessage(context.Background(), brokerResult.BrokerAddr, pullRequest)
		if err != nil {
			rlog.Warning("pull message from broker error", map[string]interface{}{
				rlog.LogKeyBroker:        brokerResult.BrokerAddr,
				rlog.LogKeyUnderlayError: err.Error(),
			})
			sleepTime = _PullDelayTimeWhenError
			goto NEXT
		}

		if result.Status == primitive.PullBrokerTimeout {
			rlog.Warning("pull broker timeout", map[string]interface{}{
				rlog.LogKeyBroker: brokerResult.BrokerAddr,
			})
			sleepTime = _PullDelayTimeWhenError
			goto NEXT
		}

		switch result.Status {
		case primitive.PullFound:
			rlog.Debug(fmt.Sprintf("Topic: %s, QueueId: %d found messages.", request.mq.Topic, request.mq.QueueId), nil)
			prevRequestOffset := request.nextOffset
			request.nextOffset = result.NextBeginOffset

			rt := time.Now().Sub(beginTime) / time.Millisecond
			increasePullRT(pc.consumerGroup, request.mq.Topic, int64(rt))

			pc.processPullResult(request.mq, result, sd)

			msgFounded := result.GetMessageExts()
			firstMsgOffset := int64(math.MaxInt64)
			if msgFounded != nil && len(msgFounded) != 0 {
				firstMsgOffset = msgFounded[0].QueueOffset
				increasePullTPS(pc.consumerGroup, request.mq.Topic, len(msgFounded))
				pq.putMessage(msgFounded...)
			}
			if result.NextBeginOffset < prevRequestOffset || firstMsgOffset < prevRequestOffset {
				rlog.Warning("[BUG] pull message result maybe data wrong", map[string]interface{}{
					"nextBeginOffset":   result.NextBeginOffset,
					"firstMsgOffset":    firstMsgOffset,
					"prevRequestOffset": prevRequestOffset,
				})
			}
		case primitive.PullNoNewMsg:
			rlog.Debug(fmt.Sprintf("Topic: %s, QueueId: %d no more msg, current offset: %d, next offset: %d",
				request.mq.Topic, request.mq.QueueId, pullRequest.QueueOffset, result.NextBeginOffset), nil)
		case primitive.PullNoMsgMatched:
			request.nextOffset = result.NextBeginOffset
			pc.correctTagsOffset(request)
		case primitive.PullOffsetIllegal:
			rlog.Warning("the pull request offset illegal", map[string]interface{}{
				rlog.LogKeyPullRequest: request.String(),
				"result":               result.String(),
			})
			request.nextOffset = result.NextBeginOffset
			pq.WithDropped(true)
			go primitive.WithRecover(func() {
				time.Sleep(10 * time.Second)
				pc.storage.update(request.mq, request.nextOffset, false)
				pc.storage.persist([]*primitive.MessageQueue{request.mq})
				pc.storage.remove(request.mq)
				rlog.Warning(fmt.Sprintf("fix the pull request offset: %s", request.String()), nil)
			})
		default:
			rlog.Warning(fmt.Sprintf("unknown pull status: %v", result.Status), nil)
			sleepTime = _PullDelayTimeWhenError
		}
	}
}

func (pc *pushConsumer) correctTagsOffset(pr *PullRequest) {
	// TODO
}

func (pc *pushConsumer) sendMessageBack(brokerName string, msg *primitive.MessageExt, delayLevel int) bool {
	var brokerAddr string
	if len(brokerName) != 0 {
		brokerAddr = pc.defaultConsumer.namesrv.FindBrokerAddrByName(brokerName)
	} else {
		brokerAddr = msg.StoreHost
	}
	_, err := pc.client.InvokeSync(context.Background(), brokerAddr, pc.buildSendBackRequest(msg, delayLevel), 3*time.Second)
	if err != nil {
		return false
	}
	return true
}

func (pc *pushConsumer) buildSendBackRequest(msg *primitive.MessageExt, delayLevel int) *remote.RemotingCommand {
	req := &internal.ConsumerSendMsgBackRequestHeader{
		Group:             pc.consumerGroup,
		OriginTopic:       msg.Topic,
		Offset:            msg.CommitLogOffset,
		DelayLevel:        delayLevel,
		OriginMsgId:       msg.MsgId,
		MaxReconsumeTimes: pc.getMaxReconsumeTimes(),
	}

	return remote.NewRemotingCommand(internal.ReqConsumerSendMsgBack, req, msg.Body)
}

func (pc *pushConsumer) suspend() {
	pc.pause = true
	rlog.Info(fmt.Sprintf("suspend consumer: %s", pc.consumerGroup), nil)
}

func (pc *pushConsumer) resume() {
	pc.pause = false
	pc.doBalance()
	rlog.Info(fmt.Sprintf("resume consumer: %s", pc.consumerGroup), nil)
}

func (pc *pushConsumer) resetOffset(topic string, table map[primitive.MessageQueue]int64) {
	//topic := cmd.ExtFields["topic"]
	//group := cmd.ExtFields["group"]
	//if topic == "" || group == "" {
	//	rlog.Warning("received reset offset command from: %s, but missing params.", from)
	//	return
	//}
	//t, err := strconv.ParseInt(cmd.ExtFields["timestamp"], 10, 64)
	//if err != nil {
	//	rlog.Warning("received reset offset command from: %s, but parse time error: %s", err.Error())
	//	return
	//}
	//rlog.Infof("invoke reset offset operation from broker. brokerAddr=%s, topic=%s, group=%s, timestamp=%v",
	//	from, topic, group, t)
	//
	//offsetTable := make(map[MessageQueue]int64, 0)
	//err = json.Unmarshal(cmd.Body, &offsetTable)
	//if err != nil {
	//	rlog.Warning("received reset offset command from: %s, but parse offset table: %s", err.Error())
	//	return
	//}
	//v, exist := c.consumerMap.Load(group)
	//if !exist {
	//	rlog.Infof("[reset-offset] consumer dose not exist. group=%s", group)
	//	return
	//}

	pc.processQueueTable.Range(func(key, value interface{}) bool {
		mq := key.(primitive.MessageQueue)
		pq := value.(*processQueue)
		if _, ok := table[mq]; !ok {
			pq.WithDropped(true)
			pq.clear()
		}
		return true
	})
	time.Sleep(10 * time.Second)
	v, exist := pc.topicSubscribeInfoTable.Load(topic)
	if !exist {
		return
	}
	queuesOfTopic := v.([]primitive.MessageQueue)
	for _, k := range queuesOfTopic {
		if _, ok := table[k]; ok {
			pc.storage.update(&k, table[k], false)
			v, exist := pc.processQueueTable.Load(k)
			if !exist {
				continue
			}
			pq := v.(*processQueue)
			pc.removeUnnecessaryMessageQueue(&k, pq)
		}
	}
}

func (pc *pushConsumer) removeUnnecessaryMessageQueue(mq *primitive.MessageQueue, pq *processQueue) bool {
	pc.defaultConsumer.removeUnnecessaryMessageQueue(mq, pq)
	if !pc.consumeOrderly || Clustering != pc.model {
		return true
	}
	// TODO orderly
	return true
}

func (pc *pushConsumer) consumeInner(ctx context.Context, subMsgs []*primitive.MessageExt) (ConsumeResult, error) {
	if len(subMsgs) == 0 {
		return ConsumeRetryLater, errors.New("msg list empty")
	}

	f, exist := pc.consumeFunc.Contains(subMsgs[0].Topic)
	if !exist {
		return ConsumeRetryLater, fmt.Errorf("the consume callback missing for topic: %s", subMsgs[0].Topic)
	}

	callback, ok := f.(*PushConsumerCallback)
	if !ok {
		return ConsumeRetryLater, fmt.Errorf("the consume callback assert failed for topic: %s", subMsgs[0].Topic)
	}
	if pc.interceptor == nil {
		return callback.f(ctx, subMsgs...)
	} else {
		var container ConsumeResultHolder
		err := pc.interceptor(ctx, subMsgs, &container, func(ctx context.Context, req, reply interface{}) error {
			msgs := req.([]*primitive.MessageExt)
			r, e := callback.f(ctx, msgs...)

			realReply := reply.(*ConsumeResultHolder)
			realReply.ConsumeResult = r

			msgCtx, _ := primitive.GetConsumerCtx(ctx)
			msgCtx.Success = realReply.ConsumeResult == ConsumeSuccess
			return e
		})
		return container.ConsumeResult, err
	}
}

// resetRetryAndNamespace modify retry message.
func (pc *pushConsumer) resetRetryAndNamespace(subMsgs []*primitive.MessageExt) {
	groupTopic := internal.RetryGroupTopicPrefix + pc.consumerGroup
	beginTime := time.Now()
	for idx := range subMsgs {
		msg := subMsgs[idx]
		retryTopic := msg.GetProperty(primitive.PropertyRetryTopic)
		if retryTopic == "" && groupTopic == msg.Topic {
			msg.Topic = retryTopic
		}
		subMsgs[idx].WithProperty(primitive.PropertyConsumeStartTime, strconv.FormatInt(
			beginTime.UnixNano()/int64(time.Millisecond), 10))
	}
}

func (pc *pushConsumer) consumeMessageCurrently(pq *processQueue, mq *primitive.MessageQueue) {
	msgs := pq.getMessages()
	if msgs == nil {
		return
	}
	for count := 0; count < len(msgs); count++ {
		var subMsgs []*primitive.MessageExt
		if count+pc.option.ConsumeMessageBatchMaxSize > len(msgs) {
			subMsgs = msgs[count:]
			count = len(msgs)
		} else {
			next := count + pc.option.ConsumeMessageBatchMaxSize
			subMsgs = msgs[count:next]
			count = next - 1
		}
		go primitive.WithRecover(func() {
		RETRY:
			if pq.IsDroppd() {
				rlog.Info("the message queue not be able to consume, because it was dropped", map[string]interface{}{
					rlog.LogKeyMessageQueue:  mq.String(),
					rlog.LogKeyConsumerGroup: pc.consumerGroup,
				})
				return
			}

			beginTime := time.Now()
			pc.resetRetryAndNamespace(subMsgs)
			var result ConsumeResult

			var err error
			msgCtx := &primitive.ConsumeMessageContext{
				Properties:    make(map[string]string),
				ConsumerGroup: pc.consumerGroup,
				MQ:            mq,
				Msgs:          msgs,
			}
			ctx := context.Background()
			ctx = primitive.WithConsumerCtx(ctx, msgCtx)
			ctx = primitive.WithMethod(ctx, primitive.ConsumerPush)
			concurrentCtx := primitive.NewConsumeConcurrentlyContext()
			concurrentCtx.MQ = *mq
			ctx = primitive.WithConcurrentlyCtx(ctx, concurrentCtx)

			result, err = pc.consumeInner(ctx, subMsgs)

			consumeRT := time.Now().Sub(beginTime)
			if err != nil {
				msgCtx.Properties[primitive.PropCtxType] = string(primitive.ExceptionReturn)
			} else if consumeRT >= pc.option.ConsumeTimeout {
				msgCtx.Properties[primitive.PropCtxType] = string(primitive.TimeoutReturn)
			} else if result == ConsumeSuccess {
				msgCtx.Properties[primitive.PropCtxType] = string(primitive.SuccessReturn)
			} else if result == ConsumeRetryLater {
				msgCtx.Properties[primitive.PropCtxType] = string(primitive.FailedReturn)
			}

			increaseConsumeRT(pc.consumerGroup, mq.Topic, int64(consumeRT/time.Millisecond))

			if !pq.IsDroppd() {
				msgBackFailed := make([]*primitive.MessageExt, 0)
				if result == ConsumeSuccess {
					increaseConsumeOKTPS(pc.consumerGroup, mq.Topic, len(subMsgs))
				} else {
					increaseConsumeFailedTPS(pc.consumerGroup, mq.Topic, len(subMsgs))
					if pc.model == BroadCasting {
						for i := 0; i < len(msgs); i++ {
							rlog.Warning("BROADCASTING, the message consume failed, drop it", map[string]interface{}{
								"message": subMsgs[i],
							})
						}
					} else {
						for i := 0; i < len(msgs); i++ {
							msg := msgs[i]
							if !pc.sendMessageBack(mq.BrokerName, msg, concurrentCtx.DelayLevelWhenNextConsume) {
								msg.ReconsumeTimes += 1
								msgBackFailed = append(msgBackFailed, msg)
							}
						}
					}
				}

				offset := pq.removeMessage(subMsgs...)

				if offset >= 0 && !pq.IsDroppd() {
					pc.storage.update(mq, int64(offset), true)
				}
				if len(msgBackFailed) > 0 {
					subMsgs = msgBackFailed
					time.Sleep(5 * time.Second)
					goto RETRY
				}
			} else {
				rlog.Warning("processQueue is dropped without process consume result.", map[string]interface{}{
					rlog.LogKeyMessageQueue: mq,
					"message":               msgs,
				})
			}
		})
	}
}

func (pc *pushConsumer) consumeMessageOrderly(pq *processQueue, mq *primitive.MessageQueue) {
	if pq.IsDroppd() {
		rlog.Warning("the message queue not be able to consume, because it's dropped.", map[string]interface{}{
			rlog.LogKeyMessageQueue: mq.String(),
		})
		return
	}

	lock := pc.queueLock.fetchLock(*mq)
	lock.Lock()
	defer lock.Unlock()
	if pc.model == BroadCasting || (pq.IsLock() && !pq.isLockExpired()) {
		beginTime := time.Now()

		continueConsume := true
		for continueConsume {
			if pq.IsDroppd() {
				rlog.Warning("the message queue not be able to consume, because it's dropped.", map[string]interface{}{
					rlog.LogKeyMessageQueue: mq.String(),
				})
				break
			}
			if pc.model == Clustering {
				if !pq.IsLock() {
					rlog.Warning("the message queue not locked, so consume later", map[string]interface{}{
						rlog.LogKeyMessageQueue: mq.String(),
					})
					pc.tryLockLaterAndReconsume(mq, 10)
					return
				}
				if pq.isLockExpired() {
					rlog.Warning("the message queue lock expired, so consume later", map[string]interface{}{
						rlog.LogKeyMessageQueue: mq.String(),
					})
					pc.tryLockLaterAndReconsume(mq, 10)
					return
				}
			}
			interval := time.Now().Sub(beginTime)
			if interval > pc.option.MaxTimeConsumeContinuously {
				time.Sleep(10 * time.Millisecond)
				return
			}
			batchSize := pc.option.ConsumeMessageBatchMaxSize
			msgs := pq.takeMessages(batchSize)

			pc.resetRetryAndNamespace(msgs)

			if len(msgs) == 0 {
				continueConsume = false
				break
			}

			// TODO: add message consumer hook
			beginTime = time.Now()

			ctx := context.Background()
			msgCtx := &primitive.ConsumeMessageContext{
				Properties:    make(map[string]string),
				ConsumerGroup: pc.consumerGroup,
				MQ:            mq,
				Msgs:          msgs,
			}
			ctx = primitive.WithConsumerCtx(ctx, msgCtx)
			ctx = primitive.WithMethod(ctx, primitive.ConsumerPush)

			orderlyCtx := primitive.NewConsumeOrderlyContext()
			orderlyCtx.MQ = *mq
			ctx = primitive.WithOrderlyCtx(ctx, orderlyCtx)

			pq.lockConsume.Lock()
			result, _ := pc.consumeInner(ctx, msgs)
			pq.lockConsume.Unlock()

			if result == Rollback || result == SuspendCurrentQueueAMoment {
				rlog.Warning("consumeMessage Orderly return not OK", map[string]interface{}{
					rlog.LogKeyConsumerGroup: pc.consumerGroup,
					"messages":               msgs,
					rlog.LogKeyMessageQueue:  mq,
				})
			}

			// jsut put consumeResult in consumerMessageCtx
			//interval = time.Now().Sub(beginTime)
			//consumeReult := SuccessReturn
			//if interval > pc.option.ConsumeTimeout {
			//	consumeReult = TimeoutReturn
			//} else if SuspendCurrentQueueAMoment == result {
			//	consumeReult = FailedReturn
			//} else if ConsumeSuccess == result {
			//	consumeReult = SuccessReturn
			//}

			// process result
			commitOffset := int64(-1)
			if pc.option.AutoCommit {
				switch result {
				case Commit, Rollback:
					rlog.Warning("the message queue consume result is illegal, we think you want to ack these message: %v", map[string]interface{}{
						rlog.LogKeyMessageQueue: mq,
					})
				case ConsumeSuccess:
					commitOffset = pq.commit()
				case SuspendCurrentQueueAMoment:
					if pc.checkReconsumeTimes(msgs) {
						pq.putMessage(msgs...)
						time.Sleep(time.Duration(orderlyCtx.SuspendCurrentQueueTimeMillis) * time.Millisecond)
						continueConsume = false
					} else {
						commitOffset = pq.commit()
					}
				default:
				}
			} else {
				switch result {
				case ConsumeSuccess:
				case Commit:
					commitOffset = pq.commit()
				case Rollback:
					// pq.rollback
					time.Sleep(time.Duration(orderlyCtx.SuspendCurrentQueueTimeMillis) * time.Millisecond)
					continueConsume = false
				case SuspendCurrentQueueAMoment:
					if pc.checkReconsumeTimes(msgs) {
						time.Sleep(time.Duration(orderlyCtx.SuspendCurrentQueueTimeMillis) * time.Millisecond)
						continueConsume = false
					}
				default:
				}
			}
			if commitOffset > 0 && !pq.IsDroppd() {
				_ = pc.updateOffset(mq, commitOffset)
			}
		}
	} else {
		if pq.IsDroppd() {
			rlog.Warning("the message queue not be able to consume, because it's dropped.", map[string]interface{}{
				rlog.LogKeyMessageQueue: mq.String(),
			})
		}
		pc.tryLockLaterAndReconsume(mq, 100)
	}
}

func (pc *pushConsumer) checkReconsumeTimes(msgs []*primitive.MessageExt) bool {
	suspend := false
	if len(msgs) != 0 {
		maxReconsumeTimes := pc.getOrderlyMaxReconsumeTimes()
		for _, msg := range msgs {
			if msg.ReconsumeTimes > maxReconsumeTimes {
				rlog.Warning(fmt.Sprintf("msg will be send to retry topic due to ReconsumeTimes > %d, \n", maxReconsumeTimes), nil)
				msg.WithProperty("RECONSUME_TIME", strconv.Itoa(int(msg.ReconsumeTimes)))
				if !pc.sendMessageBack("", msg, -1) {
					suspend = true
					msg.ReconsumeTimes += 1
				}
			} else {
				suspend = true
				msg.ReconsumeTimes += 1
			}
		}
	}
	return suspend
}

func (pc *pushConsumer) getOrderlyMaxReconsumeTimes() int32 {
	if pc.option.MaxReconsumeTimes == -1 {
		return math.MaxInt32
	} else {
		return pc.option.MaxReconsumeTimes
	}
}

func (pc *pushConsumer) getMaxReconsumeTimes() int32 {
	if pc.option.MaxReconsumeTimes == -1 {
		return 16
	} else {
		return pc.option.MaxReconsumeTimes
	}
}

func (pc *pushConsumer) tryLockLaterAndReconsume(mq *primitive.MessageQueue, delay int64) {
	time.Sleep(time.Duration(delay) * time.Millisecond)
	if pc.lock(mq) == true {
		pc.submitConsumeRequestLater(10)
	} else {
		pc.submitConsumeRequestLater(3000)
	}
}

func (pc *pushConsumer) submitConsumeRequestLater(suspendTimeMillis int64) {
	if suspendTimeMillis == -1 {
		suspendTimeMillis = int64(pc.option.SuspendCurrentQueueTimeMillis / time.Millisecond)
	}
	if suspendTimeMillis < 10 {
		suspendTimeMillis = 10
	} else if suspendTimeMillis > 30000 {
		suspendTimeMillis = 30000
	}
	time.Sleep(time.Duration(suspendTimeMillis) * time.Millisecond)
}
