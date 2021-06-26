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
	"sync"
	"sync/atomic"

	"github.com/pkg/errors"

	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/internal/utils"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
)

type PullConsumer interface {
	// Start
	Start()

	// Shutdown refuse all new pull operation, finish all submitted.
	Shutdown()

	// Pull pull message of topic,  selector indicate which queue to pull.
	Pull(ctx context.Context, topic string, selector MessageSelector, numbers int) (*primitive.PullResult, error)

	// PullFrom pull messages of queue from the offset to offset + numbers
	PullFrom(ctx context.Context, queue *primitive.MessageQueue, offset int64, numbers int) (*primitive.PullResult, error)

	// updateOffset update offset of queue in mem
	UpdateOffset(queue *primitive.MessageQueue, offset int64) error

	// PersistOffset persist all offset in mem.
	PersistOffset(ctx context.Context) error

	// CurrentOffset return the current offset of queue in mem.
	CurrentOffset(queue *primitive.MessageQueue) (int64, error)
}

var (
	queueCounterTable sync.Map
)

type defaultPullConsumer struct {
	*defaultConsumer

	option    consumerOptions
	client    internal.RMQClient
	GroupName string
	Model     MessageModel
	UnitMode  bool

	interceptor primitive.Interceptor
}

func NewPullConsumer(options ...Option) (*defaultPullConsumer, error) {
	defaultOpts := defaultPullConsumerOptions()
	for _, apply := range options {
		apply(&defaultOpts)
	}

	srvs, err := internal.NewNamesrv(defaultOpts.NameServerAddrs)
	if err != nil {
		return nil, errors.Wrap(err, "new Namesrv failed.")
	}

	dc := &defaultConsumer{
		client:        internal.GetOrNewRocketMQClient(defaultOpts.ClientOptions, nil),
		consumerGroup: defaultOpts.GroupName,
		cType:         _PullConsume,
		state:         int32(internal.StateCreateJust),
		prCh:          make(chan PullRequest, 4),
		model:         defaultOpts.ConsumerModel,
		option:        defaultOpts,

		namesrv: srvs,
	}

	c := &defaultPullConsumer{
		defaultConsumer: dc,
	}
	return c, nil
}

func (c *defaultPullConsumer) Start() error {
	atomic.StoreInt32(&c.state, int32(internal.StateRunning))

	var err error
	c.once.Do(func() {
		err = c.start()
		if err != nil {
			return
		}
	})

	return err
}

func (c *defaultPullConsumer) Pull(ctx context.Context, topic string, selector MessageSelector, numbers int) (*primitive.PullResult, error) {
	mq := c.getNextQueueOf(topic)
	if mq == nil {
		return nil, fmt.Errorf("prepard to pull topic: %s, but no queue is founded", topic)
	}

	data := buildSubscriptionData(mq.Topic, selector)
	result, err := c.pull(context.Background(), mq, data, c.nextOffsetOf(mq), numbers)

	if err != nil {
		return nil, err
	}

	c.processPullResult(mq, result, data)
	return result, nil
}

func (c *defaultPullConsumer) getNextQueueOf(topic string) *primitive.MessageQueue {
	queues, err := c.defaultConsumer.namesrv.FetchSubscribeMessageQueues(topic)
	if err != nil && len(queues) > 0 {
		rlog.Error("get next mq error", map[string]interface{}{
			rlog.LogKeyTopic:         topic,
			rlog.LogKeyUnderlayError: err.Error(),
		})
		return nil
	}
	var index int64
	v, exist := queueCounterTable.Load(topic)
	if !exist {
		index = -1
		queueCounterTable.Store(topic, 0)
	} else {
		index = v.(int64)
	}

	return queues[int(atomic.AddInt64(&index, 1))%len(queues)]
}

// SubscribeWithChan ack manually
func (c *defaultPullConsumer) SubscribeWithChan(topic, selector MessageSelector) (chan *primitive.Message, error) {
	return nil, nil
}

// SubscribeWithFunc ack automatic
func (c *defaultPullConsumer) SubscribeWithFunc(topic, selector MessageSelector,
	f func(msg *primitive.Message) ConsumeResult) error {
	return nil
}

func (c *defaultPullConsumer) ACK(msg *primitive.Message, result ConsumeResult) {

}

func (dc *defaultConsumer) checkPull(ctx context.Context, mq *primitive.MessageQueue, offset int64, numbers int) error {
	err := dc.makeSureStateOK()
	if err != nil {
		return err
	}

	if mq == nil {
		return utils.ErrMQEmpty
	}

	if offset < 0 {
		return utils.ErrOffset
	}

	if numbers <= 0 {
		return utils.ErrNumbers
	}
	return nil
}

// TODO: add timeout limit
// TODO: add hook
func (c *defaultPullConsumer) pull(ctx context.Context, mq *primitive.MessageQueue, data *internal.SubscriptionData,
	offset int64, numbers int) (*primitive.PullResult, error) {

	if err := c.checkPull(ctx, mq, offset, numbers); err != nil {
		return nil, err
	}

	c.subscriptionAutomatically(mq.Topic)

	sysFlag := buildSysFlag(false, true, true, false)

	pullResp, err := c.pullInner(ctx, mq, data, offset, numbers, sysFlag, 0)
	if err != nil {
		return nil, err
	}
	c.processPullResult(mq, pullResp, data)

	return pullResp, err
}

func (c *defaultPullConsumer) makeSureStateOK() error {
	if atomic.LoadInt32(&c.state) != int32(internal.StateRunning) {
		return fmt.Errorf("the consumer state is [%d], not running", c.state)
	}
	return nil
}

func (c *defaultPullConsumer) nextOffsetOf(queue *primitive.MessageQueue) int64 {
	return c.computePullFromWhere(queue)
}

// PullFrom pull messages of queue from the offset to offset + numbers
func (c *defaultPullConsumer) PullFrom(ctx context.Context, queue *primitive.MessageQueue, offset int64, numbers int) (*primitive.PullResult, error) {
	if err := c.checkPull(ctx, queue, offset, numbers); err != nil {
		return nil, err
	}

	selector := MessageSelector{}
	data := buildSubscriptionData(queue.Topic, selector)

	return c.pull(ctx, queue, data, offset, numbers)
}

// updateOffset update offset of queue in mem
func (c *defaultPullConsumer) UpdateOffset(queue *primitive.MessageQueue, offset int64) error {
	return c.updateOffset(queue, offset)
}

// PersistOffset persist all offset in mem.
func (c *defaultPullConsumer) PersistOffset(ctx context.Context) error {
	return c.persistConsumerOffset()
}

// CurrentOffset return the current offset of queue in mem.
func (c *defaultPullConsumer) CurrentOffset(queue *primitive.MessageQueue) (int64, error) {
	v := c.queryOffset(queue)
	return v, nil
}

// Shutdown close defaultConsumer, refuse new request.
func (c *defaultPullConsumer) Shutdown() error {
	return c.defaultConsumer.shutdown()
}
