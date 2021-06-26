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

package internal

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	. "github.com/smartystreets/goconvey/convey"
	"github.com/tidwall/gjson"

	"github.com/apache/rocketmq-client-go/internal/utils"
	"github.com/apache/rocketmq-client-go/primitive"
)

func TestHeartbeatData(t *testing.T) {
	Convey("test heatbeat json", t, func() {

		Convey("producerData set marshal", func() {
			pData := &producerData{
				GroupName: "group name",
			}
			pData2 := &producerData{
				GroupName: "group name 2",
			}
			set := utils.NewSet()
			set.Add(pData)
			set.Add(pData2)

			v, err := json.Marshal(set)
			So(err, ShouldBeNil)
			fmt.Printf("json producer set: %s", string(v))
		})

		Convey("producer heatbeat", func() {

			hbt := NewHeartbeatData("producer client id")
			p1 := &producerData{
				GroupName: "group name",
			}
			p2 := &producerData{
				GroupName: "group name 2",
			}

			hbt.ProducerDatas.Add(p1)
			hbt.ProducerDatas.Add(p2)

			v, err := json.Marshal(hbt)
			So(err, ShouldBeNil)
			fmt.Printf("json producer: %s\n", string(v))
		})

		Convey("consumer heartbeat", func() {

			hbt := NewHeartbeatData("consumer client id")
			c1 := consumerData{
				GroupName: "consumer data 1",
			}
			c2 := consumerData{
				GroupName: "consumer data 2",
			}
			hbt.ConsumerDatas.Add(c1)
			hbt.ConsumerDatas.Add(c2)

			v, err := json.Marshal(hbt)
			So(err, ShouldBeNil)
			fmt.Printf("json consumer: %s\n", string(v))
		})

		Convey("producer & consumer heartbeat", func() {

			hbt := NewHeartbeatData("consumer client id")

			p1 := &producerData{
				GroupName: "group name",
			}
			p2 := &producerData{
				GroupName: "group name 2",
			}

			hbt.ProducerDatas.Add(p1)
			hbt.ProducerDatas.Add(p2)

			c1 := consumerData{
				GroupName: "consumer data 1",
			}
			c2 := consumerData{
				GroupName: "consumer data 2",
			}
			hbt.ConsumerDatas.Add(c1)
			hbt.ConsumerDatas.Add(c2)

			v, err := json.Marshal(hbt)
			So(err, ShouldBeNil)
			fmt.Printf("json producer & consumer: %s\n", string(v))
		})
	})

}

func TestConsumerRunningInfo_MarshalJSON(t *testing.T) {
	Convey("test ConsumerRunningInfo MarshalJson", t, func() {
		props := map[string]string{
			"maxReconsumeTimes":             "-1",
			"unitMode":                      "false",
			"adjustThreadPoolNumsThreshold": "100000",
			"consumerGroup":                 "mq-client-go-test%GID_GO_TEST",
			"messageModel":                  "CLUSTERING",
			"suspendCurrentQueueTimeMillis": "1000",
			"pullThresholdSizeForTopic":     "-1",
			"pullThresholdSizeForQueue":     "100",
			"PROP_CLIENT_VERSION":           "V4_5_1",
			"consumeConcurrentlyMaxSpan":    "2000",
			"postSubscriptionWhenPull":      "false",
			"consumeTimestamp":              "20191127013617",
			"PROP_CONSUME_TYPE":             "CONSUME_PASSIVELY",
			"consumeTimeout":                "15",
			"consumeMessageBatchMaxSize":    "1",
			"PROP_THREADPOOL_CORE_SIZE":     "20",
			"pullInterval":                  "0",
			"pullThresholdForQueue":         "1000",
			"pullThresholdForTopic":         "-1",
			"consumeFromWhere":              "CONSUME_FROM_FIRST_OFFSET",
			"PROP_NAMESERVER_ADDR":          "mq-client-go-test.mq-internet-access.mq-internet.aliyuncs.com:80;",
			"pullBatchSize":                 "32",
			"consumeThreadMin":              "20",
			"PROP_CONSUMER_START_TIMESTAMP": "1574791577504",
			"consumeThreadMax":              "20",
			"subscription":                  "{}",
			"PROP_CONSUMEORDERLY":           "false",
		}
		subData := map[*SubscriptionData]bool{
			&SubscriptionData{
				ClassFilterMode: false,
				Codes:           utils.NewSet(),
				ExpType:         "TAG",
				SubString:       "*",
				SubVersion:      1574791579242,
				Tags:            utils.NewSet(),
				Topic:           "%RETRY%mq-client-go-test%GID_GO_TEST",
			}: true,
			&SubscriptionData{
				ClassFilterMode: true,
				Codes:           utils.NewSet(),
				ExpType:         "TAG",
				SubString:       "*",
				SubVersion:      1574791577523,
				Tags:            utils.NewSet(),
				Topic:           "mq-client-go-test%go-test",
			}: true,
		}
		statusTable := map[string]ConsumeStatus{
			"%RETRY%mq-client-go-test%GID_GO_TEST": {
				PullRT:            11.11,
				PullTPS:           22.22,
				ConsumeRT:         33.33,
				ConsumeOKTPS:      44.44,
				ConsumeFailedTPS:  55.55,
				ConsumeFailedMsgs: 666,
			},
			"mq-client-go-test%go-test": {
				PullRT:            123,
				PullTPS:           123,
				ConsumeRT:         123,
				ConsumeOKTPS:      123,
				ConsumeFailedTPS:  123,
				ConsumeFailedMsgs: 1234,
			},
		}
		mqTable := map[primitive.MessageQueue]ProcessQueueInfo{
			{
				Topic:      "%RETRY%mq-client-go-test%GID_GO_TEST",
				BrokerName: "qd7internet-01",
				QueueId:    0,
			}: {
				CommitOffset:            0,
				CachedMsgMinOffset:      0,
				CachedMsgMaxOffset:      0,
				CachedMsgCount:          0,
				CachedMsgSizeInMiB:      0,
				TransactionMsgMinOffset: 0,
				TransactionMsgMaxOffset: 0,
				TransactionMsgCount:     0,
				Locked:                  false,
				TryUnlockTimes:          0,
				LastLockTimestamp:       1574791579221,
				Dropped:                 false,
				LastPullTimestamp:       1574791579242,
				LastConsumeTimestamp:    1574791579221,
			},
			{
				Topic:      "%RETRY%mq-client-go-test%GID_GO_TEST",
				BrokerName: "qd7internet-01",
				QueueId:    1,
			}: {
				CommitOffset:            1,
				CachedMsgMinOffset:      2,
				CachedMsgMaxOffset:      3,
				CachedMsgCount:          4,
				CachedMsgSizeInMiB:      5,
				TransactionMsgMinOffset: 6,
				TransactionMsgMaxOffset: 7,
				TransactionMsgCount:     8,
				Locked:                  true,
				TryUnlockTimes:          9,
				LastLockTimestamp:       1574791579221,
				Dropped:                 false,
				LastPullTimestamp:       1574791579242,
				LastConsumeTimestamp:    1574791579221,
			},
		}
		info := ConsumerRunningInfo{
			Properties:       props,
			SubscriptionData: subData,
			StatusTable:      statusTable,
			MQTable:          mqTable,
		}
		data, err := info.Encode()
		So(err, ShouldBeNil)
		result := gjson.ParseBytes(data)
		Convey("test Properties fields", func() {
			r1 := result.Get("properties")
			So(r1.Exists(), ShouldBeTrue)
			m := r1.Map()
			So(len(m), ShouldEqual, 27)

			So(m["PROP_CLIENT_VERSION"], ShouldNotBeEmpty)
			So(m["PROP_CLIENT_VERSION"].String(), ShouldEqual, "V4_5_1")

			So(m["PROP_CONSUME_TYPE"], ShouldNotBeNil)
			So(m["PROP_CONSUME_TYPE"].String(), ShouldEqual, "CONSUME_PASSIVELY")

			So(m["PROP_THREADPOOL_CORE_SIZE"], ShouldNotBeNil)
			So(m["PROP_THREADPOOL_CORE_SIZE"].String(), ShouldEqual, "20")

			So(m["PROP_NAMESERVER_ADDR"], ShouldNotBeNil)
			So(m["PROP_NAMESERVER_ADDR"].String(), ShouldEqual, "mq-client-go-test.mq-internet-access.mq-internet.aliyuncs.com:80;")

			So(m["PROP_CONSUMER_START_TIMESTAMP"], ShouldNotBeNil)
			So(m["PROP_CONSUMER_START_TIMESTAMP"].String(), ShouldEqual, "1574791577504")

			So(m["PROP_CONSUMEORDERLY"], ShouldNotBeNil)
			So(m["PROP_CONSUMEORDERLY"].String(), ShouldEqual, "false")
		})
		Convey("test SubscriptionData fields", func() {
			r2 := result.Get("subscriptionSet")
			So(r2.Exists(), ShouldBeTrue)
			arr := r2.Array()
			So(len(arr), ShouldEqual, 2)

			m1 := arr[0].Map()
			So(len(m1), ShouldEqual, 7)
			So(m1["classFilterMode"].Bool(), ShouldEqual, false)
			So(len(m1["codes"].Array()), ShouldEqual, 0)
			So(m1["expressionType"].String(), ShouldEqual, "TAG")
			So(m1["subString"].String(), ShouldEqual, "*")
			So(m1["subVersion"].Int(), ShouldEqual, 1574791579242)
			So(len(m1["tags"].Array()), ShouldEqual, 0)
			So(m1["topic"].String(), ShouldEqual, "%RETRY%mq-client-go-test%GID_GO_TEST")

			m2 := arr[1].Map()
			So(len(m2), ShouldEqual, 7)
			So(m2["classFilterMode"].Bool(), ShouldEqual, true)
			So(len(m2["codes"].Array()), ShouldEqual, 0)
			So(m2["expressionType"].String(), ShouldEqual, "TAG")
			So(m2["subString"].String(), ShouldEqual, "*")
			So(m2["subVersion"].Int(), ShouldEqual, 1574791577523)
			So(len(m2["tags"].Array()), ShouldEqual, 0)
			So(m2["topic"].String(), ShouldEqual, "mq-client-go-test%go-test")
		})
		Convey("test StatusTable fields", func() {
			r3 := result.Get("statusTable")
			So(r3.Exists(), ShouldBeTrue)
			m := r3.Map()
			So(len(m), ShouldEqual, 2)

			status1 := m["mq-client-go-test%go-test"].Map()
			So(len(status1), ShouldEqual, 6)
			So(status1["pullRT"].Float(), ShouldEqual, 123)
			So(status1["pullTPS"].Float(), ShouldEqual, 123)
			So(status1["consumeRT"].Float(), ShouldEqual, 123)
			So(status1["consumeOKTPS"].Float(), ShouldEqual, 123)
			So(status1["consumeFailedTPS"].Float(), ShouldEqual, 123)
			So(status1["consumeFailedMsgs"].Int(), ShouldEqual, 1234)

			status2 := m["%RETRY%mq-client-go-test%GID_GO_TEST"].Map()
			So(len(status2), ShouldEqual, 6)
			So(status2["pullRT"].Float(), ShouldEqual, 11.11)
			So(status2["pullTPS"].Float(), ShouldEqual, 22.22)
			So(status2["consumeRT"].Float(), ShouldEqual, 33.33)
			So(status2["consumeOKTPS"].Float(), ShouldEqual, 44.44)
			So(status2["consumeFailedTPS"].Float(), ShouldEqual, 55.55)
			So(status2["consumeFailedMsgs"].Int(), ShouldEqual, 666)
		})
		Convey("test MQTable fields", func() {
			r4 := result.Get("mqTable")
			So(r4.Exists(), ShouldBeTrue)
			objNumbers := strings.Split(r4.String(), "},{")
			So(len(objNumbers), ShouldEqual, 2)

			obj1Str := objNumbers[0][1:len(objNumbers[0])] + "}"
			obj1KV := strings.Split(obj1Str, "}:{")
			So(len(obj1KV), ShouldEqual, 2)

			obj1 := gjson.Parse("{" + obj1KV[1][0:len(obj1KV[1])])
			So(obj1.Exists(), ShouldBeTrue)
			obj1M := obj1.Map()
			So(len(obj1M), ShouldEqual, 14)
			So(obj1M["commitOffset"].Int(), ShouldEqual, 0)
			So(obj1M["cachedMsgMinOffset"].Int(), ShouldEqual, 0)
			So(obj1M["cachedMsgMaxOffset"].Int(), ShouldEqual, 0)
			So(obj1M["cachedMsgCount"].Int(), ShouldEqual, 0)
			So(obj1M["cachedMsgSizeInMiB"].Int(), ShouldEqual, 0)
			So(obj1M["transactionMsgMinOffset"].Int(), ShouldEqual, 0)
			So(obj1M["transactionMsgMaxOffset"].Int(), ShouldEqual, 0)
			So(obj1M["transactionMsgCount"].Int(), ShouldEqual, 0)
			So(obj1M["locked"].Bool(), ShouldEqual, false)
			So(obj1M["tryUnlockTimes"].Int(), ShouldEqual, 0)
			So(obj1M["lastLockTimestamp"].Int(), ShouldEqual, 1574791579221)
			So(obj1M["dropped"].Bool(), ShouldEqual, false)
			So(obj1M["lastPullTimestamp"].Int(), ShouldEqual, 1574791579242)
			So(obj1M["lastConsumeTimestamp"].Int(), ShouldEqual, 1574791579221)

			obj2Str := "{" + objNumbers[1][0:len(objNumbers[1])-1]
			obj2KV := strings.Split(obj2Str, "}:{")
			So(len(obj2KV), ShouldEqual, 2)
			obj2 := gjson.Parse("{" + obj2KV[1][0:len(obj2KV[1])])
			So(obj2.Exists(), ShouldBeTrue)
			obj2M := obj2.Map()
			So(len(obj2M), ShouldEqual, 14)
			So(obj2M["commitOffset"].Int(), ShouldEqual, 1)
			So(obj2M["cachedMsgMinOffset"].Int(), ShouldEqual, 2)
			So(obj2M["cachedMsgMaxOffset"].Int(), ShouldEqual, 3)
			So(obj2M["cachedMsgCount"].Int(), ShouldEqual, 4)
			So(obj2M["cachedMsgSizeInMiB"].Int(), ShouldEqual, 5)
			So(obj2M["transactionMsgMinOffset"].Int(), ShouldEqual, 6)
			So(obj2M["transactionMsgMaxOffset"].Int(), ShouldEqual, 7)
			So(obj2M["transactionMsgCount"].Int(), ShouldEqual, 8)
			So(obj2M["locked"].Bool(), ShouldEqual, true)
			So(obj2M["tryUnlockTimes"].Int(), ShouldEqual, 9)
			So(obj2M["lastLockTimestamp"].Int(), ShouldEqual, 1574791579221)
			So(obj2M["dropped"].Bool(), ShouldEqual, false)
			So(obj2M["lastPullTimestamp"].Int(), ShouldEqual, 1574791579242)
			So(obj2M["lastConsumeTimestamp"].Int(), ShouldEqual, 1574791579221)
		})
	})
}
