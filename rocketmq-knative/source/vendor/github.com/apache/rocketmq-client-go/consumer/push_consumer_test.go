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
	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/golang/mock/gomock"
	. "github.com/smartystreets/goconvey/convey"
	"testing"
)

func mockB4Start(c *pushConsumer) {
	c.topicSubscribeInfoTable.Store("TopicTest", []*primitive.MessageQueue{})
}

func TestStart(t *testing.T) {
	Convey("test Start method", t, func() {
		c, _ := NewPushConsumer(
			WithGroupName("testGroup"),
			WithNameServer([]string{"127.0.0.1:9876"}),
			WithConsumerModel(BroadCasting),
		)

		ctrl := gomock.NewController(t)
		defer ctrl.Finish()

		client := internal.NewMockRMQClient(ctrl)
		c.client = client

		err := c.Subscribe("TopicTest", MessageSelector{}, func(ctx context.Context,
			msgs ...*primitive.MessageExt) (ConsumeResult, error) {
			fmt.Printf("subscribe callback: %v \n", msgs)
			return ConsumeSuccess, nil
		})

		client.EXPECT().ClientID().Return("127.0.0.1@DEFAULT")
		client.EXPECT().Start().Return()
		client.EXPECT().RegisterConsumer(gomock.Any(), gomock.Any()).Return(nil)
		client.EXPECT().UpdateTopicRouteInfo().AnyTimes().Return()

		Convey("test topic route info not found", func() {
			client.EXPECT().Shutdown().Return()
			err = c.Start()
			So(err.Error(), ShouldContainSubstring, "route info not found")
		})

		Convey("test topic route info found", func() {
			client.EXPECT().RebalanceImmediately().Return()
			client.EXPECT().CheckClientInBroker().Return()
			client.EXPECT().SendHeartbeatToAllBrokerWithLock().Return()
			mockB4Start(c)
			err = c.Start()
			So(err, ShouldBeNil)
		})
	})
}
