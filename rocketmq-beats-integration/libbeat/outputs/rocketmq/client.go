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

package rocketmq

import (
	"context"
	"strings"
	"time"

	"github.com/apache/rocketmq-client-go/v2"
	"github.com/apache/rocketmq-client-go/v2/primitive"
	"github.com/apache/rocketmq-client-go/v2/producer"
	"github.com/elastic/beats/v7/libbeat/logp"
	"github.com/elastic/beats/v7/libbeat/outputs"
	"github.com/elastic/beats/v7/libbeat/outputs/codec"
	"github.com/elastic/beats/v7/libbeat/publisher"
)

type client struct {
	log         *logp.Logger
	observer    outputs.Observer
	index       string
	codec       codec.Codec
	namesrvAddr []string
	topic       string
	group       string
	timeout     time.Duration
	maxRetries  int

	producer rocketmq.Producer
}

func (c *client) Connect() error {
	c.log.Warnf("connecting: %v", c.namesrvAddr)

	p, err := rocketmq.NewProducer(
		producer.WithNsResolver(primitive.NewPassthroughResolver(c.namesrvAddr)),
		producer.WithRetry(c.maxRetries),
		producer.WithSendMsgTimeout(c.timeout),
		producer.WithGroupName(c.group),
	)
	if err != nil {
		c.log.Errorf("RocketMQ creates producer fails with: %v", err)
		return err
	}

	errStart := p.Start()
	if errStart != nil {
		c.log.Errorf("RocketMQ starts producer fails with: %v", errStart)
		return errStart
	}

	c.producer = p

	return nil
}

func (c *client) Close() error {
	c.log.Warn("Enter Close(), shutdown...")
	if c.producer != nil {
		c.producer.Shutdown()
		c.producer = nil
	}

	return nil
}

func (c *client) Publish(_ context.Context, batch publisher.Batch) error {

	defer batch.ACK()
	st := c.observer
	events := batch.Events()
	st.NewBatch(len(events))

	dropped := 0
	for i := range events {
		d := &events[i]

		ok := c.publishEvent(d)
		if !ok {
			dropped++
		}
	}

	return nil
}

func (c *client) publishEvent(event *publisher.Event) bool {
	serializedEvent, err := c.codec.Encode(c.index, &event.Content)
	if err != nil {
		c.observer.Dropped(1)

		if !event.Guaranteed() {
			return false
		}
		c.log.Errorf("Unable to encode event: %v", err)
		return false
	}

	c.observer.WriteBytes(len(serializedEvent) + 1)

	// str := string(serializedEvent)
	// c.log.Warnf("Processing event: %v", str)

	buf := make([]byte, len(serializedEvent))
	copy(buf, serializedEvent)

	msg := &primitive.Message{
		Topic: c.topic,
		Body:  buf,
	}

	// res, err := c.producer.SendSync(context.Background(), msg)

	// if err != nil {
	// 	c.log.Errorf("send to rocketmq  is error %v", err)
	// 	return false
	// } else {
	// 	c.log.Warnf("send msg result=%v", res.String())
	// }

	err = c.producer.SendAsync(context.Background(),
		func(ctx context.Context, result *primitive.SendResult, e error) {
			if e != nil {
				c.observer.Dropped(1)
				c.log.Errorf("send to rocketmq is error %v", e)
			} else {
				c.observer.Acked(1)
				c.log.Debugf("send msg result=%v", result.String())
			}
		}, msg)

	if err != nil {
		c.observer.Dropped(1)
		c.log.Errorf("send to rocketmq is error %v", err)
		return false
	}

	return true
}

func (c *client) String() string {
	return "rocketmq[" + strings.Join(c.namesrvAddr, ",") + "]"
}
