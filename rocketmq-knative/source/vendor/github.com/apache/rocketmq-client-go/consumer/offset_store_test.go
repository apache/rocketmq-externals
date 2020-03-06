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
	"path/filepath"
	"testing"

	"github.com/golang/mock/gomock"
	. "github.com/smartystreets/goconvey/convey"

	"github.com/apache/rocketmq-client-go/internal"
	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/primitive"
)

func TestNewLocalFileOffsetStore(t *testing.T) {
	Convey("Given some test cases", t, func() {
		type testCase struct {
			clientId       string
			group          string
			expectedResult *localFileOffsetStore
		}
		cases := []testCase{
			{
				clientId: "",
				group:    "testGroup",
				expectedResult: &localFileOffsetStore{
					group: "testGroup",
					path:  filepath.Join(_LocalOffsetStorePath, "/testGroup/offset.json"),
				},
			}, {
				clientId: "192.168.24.1@default",
				group:    "",
				expectedResult: &localFileOffsetStore{
					group: "",
					path:  filepath.Join(_LocalOffsetStorePath, "/192.168.24.1@default/offset.json"),
				},
			}, {
				clientId: "192.168.24.1@default",
				group:    "testGroup",
				expectedResult: &localFileOffsetStore{
					group: "testGroup",
					path:  filepath.Join(_LocalOffsetStorePath, "/192.168.24.1@default/testGroup/offset.json"),
				},
			},
		}

		for _, value := range cases {
			result := NewLocalFileOffsetStore(value.clientId, value.group).(*localFileOffsetStore)
			value.expectedResult.OffsetTable = result.OffsetTable
			So(result, ShouldResemble, value.expectedResult)
		}
	})
}

func TestLocalFileOffsetStore(t *testing.T) {
	Convey("Given a local store with a starting value", t, func() {
		localStore := NewLocalFileOffsetStore("192.168.24.1@default", "testGroup")

		type offsetCase struct {
			queue          *primitive.MessageQueue
			setOffset      int64
			expectedOffset int64
		}
		mq := &primitive.MessageQueue{
			Topic:      "testTopic",
			BrokerName: "default",
			QueueId:    1,
		}

		Convey("test update", func() {
			Convey("when increaseOnly is false", func() {
				cases := []offsetCase{
					{
						queue:          mq,
						setOffset:      3,
						expectedOffset: 3,
					}, {
						queue:          mq,
						setOffset:      1,
						expectedOffset: 1,
					},
				}
				for _, value := range cases {
					localStore.update(value.queue, value.setOffset, false)
					offset := localStore.read(value.queue, _ReadFromMemory)
					So(offset, ShouldEqual, value.expectedOffset)
				}
			})

			Convey("when increaseOnly is true", func() {
				localStore.update(mq, 0, false)

				cases := []offsetCase{
					{
						queue:          mq,
						setOffset:      3,
						expectedOffset: 3,
					}, {
						queue:          mq,
						setOffset:      1,
						expectedOffset: 3,
					},
				}
				for _, value := range cases {
					localStore.update(value.queue, value.setOffset, true)
					offset := localStore.read(value.queue, _ReadFromMemory)
					So(offset, ShouldEqual, value.expectedOffset)
				}
			})
		})

		Convey("test persist", func() {
			localStore.update(mq, 1, false)
			offset := localStore.read(mq, _ReadFromMemory)
			So(offset, ShouldEqual, 1)

			queues := []*primitive.MessageQueue{mq}
			localStore.persist(queues)
			offset = localStore.read(mq, _ReadFromStore)
			So(offset, ShouldEqual, 1)

			delete(localStore.(*localFileOffsetStore).OffsetTable, MessageQueueKey(*mq))
			offset = localStore.read(mq, _ReadMemoryThenStore)
			So(offset, ShouldEqual, 1)
		})
	})
}

func TestRemoteBrokerOffsetStore(t *testing.T) {
	Convey("Given a remote store with a starting value", t, func() {
		ctrl := gomock.NewController(t)
		defer ctrl.Finish()

		namesrv := internal.NewMockNamesrvs(ctrl)

		rmqClient := internal.NewMockRMQClient(ctrl)
		remoteStore := NewRemoteOffsetStore("testGroup", rmqClient, namesrv)

		type offsetCase struct {
			queue          *primitive.MessageQueue
			setOffset      int64
			expectedOffset int64
		}
		mq := &primitive.MessageQueue{
			Topic:      "testTopic",
			BrokerName: "default",
			QueueId:    1,
		}

		Convey("test update", func() {
			Convey("when increaseOnly is false", func() {
				cases := []offsetCase{
					{
						queue:          mq,
						setOffset:      3,
						expectedOffset: 3,
					}, {
						queue:          mq,
						setOffset:      1,
						expectedOffset: 1,
					},
				}
				for _, value := range cases {
					remoteStore.update(value.queue, value.setOffset, false)
					offset := remoteStore.read(value.queue, _ReadFromMemory)
					So(offset, ShouldEqual, value.expectedOffset)
				}
			})

			Convey("when increaseOnly is true", func() {
				remoteStore.update(mq, 0, false)

				cases := []offsetCase{
					{
						queue:          mq,
						setOffset:      3,
						expectedOffset: 3,
					}, {
						queue:          mq,
						setOffset:      1,
						expectedOffset: 3,
					},
				}
				for _, value := range cases {
					remoteStore.update(value.queue, value.setOffset, true)
					offset := remoteStore.read(value.queue, _ReadFromMemory)
					So(offset, ShouldEqual, value.expectedOffset)
				}
			})
		})

		Convey("test persist", func() {
			queues := []*primitive.MessageQueue{mq}

			namesrv.EXPECT().FindBrokerAddrByName(gomock.Any()).Return("192.168.24.1:10911").MaxTimes(2)

			ret := &remote.RemotingCommand{
				Code: internal.ResSuccess,
				ExtFields: map[string]string{
					"offset": "1",
				},
			}
			rmqClient.EXPECT().InvokeSync(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return(ret, nil).MaxTimes(2)

			remoteStore.persist(queues)
			offset := remoteStore.read(mq, _ReadFromStore)
			So(offset, ShouldEqual, 1)

			remoteStore.remove(mq)
			offset = remoteStore.read(mq, _ReadFromMemory)
			So(offset, ShouldEqual, -1)
			offset = remoteStore.read(mq, _ReadMemoryThenStore)
			So(offset, ShouldEqual, 1)

		})

		Convey("test remove", func() {
			remoteStore.update(mq, 1, false)
			offset := remoteStore.read(mq, _ReadFromMemory)
			So(offset, ShouldEqual, 1)

			remoteStore.remove(mq)
			offset = remoteStore.read(mq, _ReadFromMemory)
			So(offset, ShouldEqual, -1)
		})
	})
}
