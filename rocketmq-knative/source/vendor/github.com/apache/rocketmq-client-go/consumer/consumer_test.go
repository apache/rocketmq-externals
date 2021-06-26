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
	"sync"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	. "github.com/smartystreets/goconvey/convey"
	"github.com/stretchr/testify/assert"

	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/primitive"
)

func TestParseTimestamp(t *testing.T) {
	layout := "20060102150405"
	timestamp, err := time.ParseInLocation(layout, "20190430193409", time.UTC)
	assert.Nil(t, err)
	assert.Equal(t, int64(1556652849), timestamp.Unix())
}

func TestDoRebalance(t *testing.T) {
	Convey("Given a defaultConsumer", t, func() {
		dc := &defaultConsumer{
			model: Clustering,
		}

		topic := "test"
		broker := "127.0.0.1:8889"
		clientID := "clientID"
		mqs := []*primitive.MessageQueue{
			{
				Topic:      topic,
				BrokerName: "",
				QueueId:    0,
			},
			{
				Topic:      topic,
				BrokerName: "",
				QueueId:    1,
			},
		}
		dc.topicSubscribeInfoTable.Store(topic, mqs)
		sub := &internal.SubscriptionData{}
		dc.subscriptionDataTable.Store(topic, sub)

		ctrl := gomock.NewController(t)
		defer ctrl.Finish()
		namesrvCli := internal.NewMockNamesrvs(ctrl)
		namesrvCli.EXPECT().FindBrokerAddrByTopic(gomock.Any()).Return(broker)
		dc.namesrv = namesrvCli

		rmqCli := internal.NewMockRMQClient(ctrl)
		rmqCli.EXPECT().InvokeSync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
			Return(&remote.RemotingCommand{
				Body: []byte("{\"consumerIdList\": [\"a1\", \"a2\", \"a3\"] }"),
			}, nil)
		rmqCli.EXPECT().ClientID().Return(clientID)
		dc.client = rmqCli

		var wg sync.WaitGroup
		wg.Add(1)
		dc.allocate = func(cg string, clientID string, mqAll []*primitive.MessageQueue, cidAll []string) []*primitive.MessageQueue {
			assert.Equal(t, cidAll, []string{"a1", "a2", "a3"})
			wg.Done()
			return nil
		}

		dc.doBalance()

		wg.Wait()
	})
}

func TestComputePullFromWhere(t *testing.T) {
	Convey("Given a defaultConsumer", t, func() {
		dc := &defaultConsumer{
			model: Clustering,
			cType: _PushConsume,
		}

		ctrl := gomock.NewController(t)
		defer ctrl.Finish()

		offsetStore := NewMockOffsetStore(ctrl)
		dc.storage = offsetStore

		mq := &primitive.MessageQueue{
			Topic: "test",
		}

		namesrvCli := internal.NewMockNamesrvs(ctrl)
		dc.namesrv = namesrvCli

		rmqCli := internal.NewMockRMQClient(ctrl)
		dc.client = rmqCli

		Convey("get effective offset", func() {
			offsetStore.EXPECT().read(gomock.Any(), gomock.Any()).Return(int64(10))
			res := dc.computePullFromWhere(mq)
			assert.Equal(t, int64(10), res)
		})

		Convey("ConsumeFromLastOffset for normal topic", func() {
			offsetStore.EXPECT().read(gomock.Any(), gomock.Any()).Return(int64(-1))
			dc.option.FromWhere = ConsumeFromLastOffset

			broker := "a"
			namesrvCli.EXPECT().FindBrokerAddrByName(gomock.Any()).Return(broker)

			rmqCli.EXPECT().InvokeSync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
				Return(&remote.RemotingCommand{
					ExtFields: map[string]string{
						"offset": "20",
					},
				}, nil)

			res := dc.computePullFromWhere(mq)
			assert.Equal(t, int64(20), res)
		})

		Convey("ConsumeFromFirstOffset for normal topic", func() {
			offsetStore.EXPECT().read(gomock.Any(), gomock.Any()).Return(int64(-1))
			dc.option.FromWhere = ConsumeFromFirstOffset

			res := dc.computePullFromWhere(mq)
			assert.Equal(t, int64(0), res)
		})

		Convey("ConsumeFromTimestamp for normal topic", func() {
			offsetStore.EXPECT().read(gomock.Any(), gomock.Any()).Return(int64(-1))
			dc.option.FromWhere = ConsumeFromTimestamp

			dc.option.ConsumeTimestamp = "20060102150405"

			broker := "a"
			namesrvCli.EXPECT().FindBrokerAddrByName(gomock.Any()).Return(broker)

			rmqCli.EXPECT().InvokeSync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
				Return(&remote.RemotingCommand{
					ExtFields: map[string]string{
						"offset": "30",
					},
				}, nil)

			res := dc.computePullFromWhere(mq)
			assert.Equal(t, int64(30), res)
		})

	})
}
