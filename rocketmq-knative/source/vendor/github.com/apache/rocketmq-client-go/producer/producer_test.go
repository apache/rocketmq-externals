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

package producer

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/primitive"
)

const (
	topic = "TopicTest"
)

func TestShutdown(t *testing.T) {
	p, _ := NewDefaultProducer(
		WithNameServer([]string{"127.0.0.1:9876"}),
		WithRetry(2),
		WithQueueSelector(NewManualQueueSelector()),
	)

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	client := internal.NewMockRMQClient(ctrl)
	p.client = client

	client.EXPECT().RegisterProducer(gomock.Any(), gomock.Any()).Return()
	client.EXPECT().Start().Return()
	err := p.Start()
	assert.Nil(t, err)

	client.EXPECT().Shutdown().Return()
	client.EXPECT().UnregisterProducer(gomock.Any()).Return()
	err = p.Shutdown()
	assert.Nil(t, err)

	ctx := context.Background()
	msg := new(primitive.Message)

	r, err := p.SendSync(ctx, msg)
	assert.Equal(t, ErrNotRunning, err)
	assert.Nil(t, r)

	err = p.SendOneWay(ctx, msg)
	assert.Equal(t, ErrNotRunning, err)

	f := func(context.Context, *primitive.SendResult, error) {
		assert.False(t, true, "should not  come in")
	}
	err = p.SendAsync(ctx, f, msg)
	assert.Equal(t, ErrNotRunning, err)
}

func mockB4Send(p *defaultProducer) {
	p.publishInfo.Store(topic, &internal.TopicPublishInfo{
		HaveTopicRouterInfo: true,
		MqList: []*primitive.MessageQueue{
			{
				Topic:      topic,
				BrokerName: "aa",
				QueueId:    0,
			},
		},
	})
	p.options.Namesrv.AddBroker(&internal.TopicRouteData{
		BrokerDataList: []*internal.BrokerData{
			{
				Cluster:    "cluster",
				BrokerName: "aa",
				BrokerAddresses: map[int64]string{
					0: "1",
				},
			},
		},
	})
}

func TestSync(t *testing.T) {
	p, _ := NewDefaultProducer(
		WithNameServer([]string{"127.0.0.1:9876"}),
		WithRetry(2),
		WithQueueSelector(NewManualQueueSelector()),
	)

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	client := internal.NewMockRMQClient(ctrl)
	p.client = client

	client.EXPECT().RegisterProducer(gomock.Any(), gomock.Any()).Return()
	client.EXPECT().Start().Return()
	err := p.Start()
	assert.Nil(t, err)

	ctx := context.Background()
	msg := &primitive.Message{
		Topic: topic,
		Body:  []byte("this is a message body"),
		Queue: &primitive.MessageQueue{
			Topic:      topic,
			BrokerName: "aa",
			QueueId:    0,
		},
	}
	msg.WithProperty("key", "value")

	expectedResp := &primitive.SendResult{
		Status:      primitive.SendOK,
		MsgID:       "111",
		QueueOffset: 0,
		OffsetMsgID: "0",
	}

	mockB4Send(p)

	client.EXPECT().InvokeSync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return(nil, nil)
	client.EXPECT().ProcessSendResponse(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Do(
		func(brokerName string, cmd *remote.RemotingCommand, resp *primitive.SendResult, msgs ...*primitive.Message) {
			resp.Status = expectedResp.Status
			resp.MsgID = expectedResp.MsgID
			resp.QueueOffset = expectedResp.QueueOffset
			resp.OffsetMsgID = expectedResp.OffsetMsgID
		})
	resp, err := p.SendSync(ctx, msg)
	assert.Nil(t, err)
	assert.Equal(t, expectedResp, resp)
}

func TestASync(t *testing.T) {
	p, _ := NewDefaultProducer(
		WithNameServer([]string{"127.0.0.1:9876"}),
		WithRetry(2),
		WithQueueSelector(NewManualQueueSelector()),
	)

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	client := internal.NewMockRMQClient(ctrl)
	p.client = client

	client.EXPECT().RegisterProducer(gomock.Any(), gomock.Any()).Return()
	client.EXPECT().Start().Return()
	err := p.Start()
	assert.Nil(t, err)

	ctx := context.Background()
	msg := &primitive.Message{
		Topic: topic,
		Body:  []byte("this is a message body"),
		Queue: &primitive.MessageQueue{
			Topic:      topic,
			BrokerName: "aa",
			QueueId:    0,
		},
	}
	msg.WithProperty("key", "value")

	expectedResp := &primitive.SendResult{
		Status:      primitive.SendOK,
		MsgID:       "111",
		QueueOffset: 0,
		OffsetMsgID: "0",
	}

	f := func(ctx context.Context, resp *primitive.SendResult, err error) {
		assert.Nil(t, err)
		assert.Equal(t, expectedResp, resp)
	}

	mockB4Send(p)

	client.EXPECT().InvokeAsync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).DoAndReturn(
		func(ctx context.Context, addr string, request *remote.RemotingCommand,
			f func(*remote.RemotingCommand, error)) error {
			// mock invoke callback
			f(nil, nil)
			return nil
		})
	client.EXPECT().ProcessSendResponse(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Do(
		func(brokerName string, cmd *remote.RemotingCommand, resp *primitive.SendResult, msgs ...*primitive.Message) {
			resp.Status = expectedResp.Status
			resp.MsgID = expectedResp.MsgID
			resp.QueueOffset = expectedResp.QueueOffset
			resp.OffsetMsgID = expectedResp.OffsetMsgID
		})

	err = p.SendAsync(ctx, f, msg)
	assert.Nil(t, err)
}

func TestOneway(t *testing.T) {
	p, _ := NewDefaultProducer(
		WithNameServer([]string{"127.0.0.1:9876"}),
		WithRetry(2),
		WithQueueSelector(NewManualQueueSelector()),
	)

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	client := internal.NewMockRMQClient(ctrl)
	p.client = client

	client.EXPECT().RegisterProducer(gomock.Any(), gomock.Any()).Return()
	client.EXPECT().Start().Return()
	err := p.Start()
	assert.Nil(t, err)

	ctx := context.Background()
	msg := &primitive.Message{
		Topic: topic,
		Body:  []byte("this is a message body"),
		Queue: &primitive.MessageQueue{
			Topic:      topic,
			BrokerName: "aa",
			QueueId:    0,
		},
	}
	msg.WithProperty("key", "value")

	mockB4Send(p)

	client.EXPECT().InvokeOneWay(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return(nil).AnyTimes()

	err = p.SendOneWay(ctx, msg)
	assert.Nil(t, err)
}
