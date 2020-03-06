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
	"testing"

	"github.com/apache/rocketmq-client-go/primitive"
	. "github.com/smartystreets/goconvey/convey"
	"github.com/stretchr/testify/assert"
)

func TestMarshal2Bean(t *testing.T) {

	Convey("marshal of TraceContext", t, func() {

		Convey("When marshal producer trace data", func() {
			traceCtx := TraceContext{
				TraceType: Pub,
				TimeStamp: 1563780533299,
				RegionId:  "DefaultRegion",
				GroupName: "ProducerGroupName",
				CostTime:  3572,
				IsSuccess: true,
				RequestId: "0A5DE93A815518B4AAC26F77F8330001",
				TraceBeans: []TraceBean{
					{
						Topic:       "TopicTest",
						MsgId:       "0A5DE93A833B18B4AAC26F842A2F0000",
						OffsetMsgId: "0A5DE93A00002A9F000000000042E322",
						Tags:        "TagA",
						Keys:        "OrderID1882",
						StoreHost:   "10.93.233.58:10911",
						ClientHost:  "10.93.233.58",
						StoreTime:   1563780535085,
						BodyLength:  11,
						MsgType:     primitive.NormalMsg,
					},
				},
			}
			bean := traceCtx.marshal2Bean()
			assert.Equal(t, "Pub1563780533299DefaultRegionProducerGroupNameTopicTest0A5DE93A833B18B4AAC26F842A2F0000TagAOrderID188210.93.233.58:1091111357200A5DE93A00002A9F000000000042E322true\x02",
				bean.transData)
			assert.Equal(t, []string{"0A5DE93A833B18B4AAC26F842A2F0000", "OrderID1882"}, bean.transKey)

			// consumer before test
			traceCtx = TraceContext{
				TraceType: SubBefore,
				TimeStamp: 1563789119096,
				GroupName: "CID_JODIE_1",
				IsSuccess: true,
				RequestId: "0A5DE93A96A818B4AAC26FFAFA780007",
				TraceBeans: []TraceBean{
					{
						Topic:      "TopicTest",
						MsgId:      "0A5DE93A973418B4AAC26FFAFA5A0000",
						Tags:       "TagA",
						Keys:       "OrderID1882",
						StoreHost:  "10.93.233.58",
						ClientHost: "10.93.233.58",
						StoreTime:  1563789119092,
						BodyLength: 190,
					},
				},
			}
			bean = traceCtx.marshal2Bean()

			Convey("transData should equal to expected", func() {
				So(bean.transData, ShouldEqual, "SubBefore1563789119096CID_JODIE_10A5DE93A96A818B4AAC26FFAFA7800070A5DE93A973418B4AAC26FFAFA5A00000OrderID1882")
			})

			Convey("transkey should equal to expected", func() {
				expectedKey := []string{"0A5DE93A973418B4AAC26FFAFA5A0000", "OrderID1882"}
				So(bean.transKey[0], ShouldEqual, expectedKey[0])
				So(bean.transKey[1], ShouldEqual, expectedKey[1])
			})
		})

		Convey("When marshal consumer trace data", func() {
			traceCtx := TraceContext{
				TraceType: SubAfter,
				TimeStamp: 1563789119096,
				GroupName: "CID_JODIE_1",
				IsSuccess: true,
				RequestId: "0A5DE93A96A818B4AAC26FFAFA780007",
				TraceBeans: []TraceBean{
					{
						Topic:      "TopicTest",
						MsgId:      "0A5DE93A973418B4AAC26FFAFA5A0000",
						Tags:       "TagA",
						Keys:       "OrderID1882",
						StoreHost:  "10.93.233.58",
						ClientHost: "10.93.233.58",
						StoreTime:  1563789119092,
						BodyLength: 190,
					},
				},
			}
			bean := traceCtx.marshal2Bean()
			Convey("transData should equal to expected", func() {
				So(bean.transData, ShouldEqual, "SubAfter0A5DE93A96A818B4AAC26FFAFA7800070A5DE93A973418B4AAC26FFAFA5A00000trueOrderID18820")
			})
			Convey("transkey should equal to expected", func() {
				expectedKey := []string{"0A5DE93A973418B4AAC26FFAFA5A0000", "OrderID1882"}
				So(bean.transKey[0], ShouldEqual, expectedKey[0])
				So(bean.transKey[1], ShouldEqual, expectedKey[1])
			})
		})
	})
}
