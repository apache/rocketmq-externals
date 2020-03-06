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
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/json-iterator/go"
	"sort"
	"strings"

	"github.com/apache/rocketmq-client-go/internal/utils"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
)

type FindBrokerResult struct {
	BrokerAddr    string
	Slave         bool
	BrokerVersion int32
}

type (
	// groupName of consumer
	consumeType string

	ServiceState int32
)

const (
	StateCreateJust ServiceState = iota
	StateStartFailed
	StateRunning
	StateShutdown
)

type SubscriptionData struct {
	ClassFilterMode bool      `json:"classFilterMode"`
	Topic           string    `json:"topic"`
	SubString       string    `json:"subString"`
	Tags            utils.Set `json:"tagsSet"`
	Codes           utils.Set `json:"codeSet"`
	SubVersion      int64     `json:"subVersion"`
	ExpType         string    `json:"expressionType"`
}

type producerData struct {
	GroupName string `json:"groupName"`
}

func (p producerData) UniqueID() string {
	return p.GroupName
}

type consumerData struct {
	GroupName         string              `json:"groupName"`
	CType             consumeType         `json:"consumeType"`
	MessageModel      string              `json:"messageModel"`
	Where             string              `json:"consumeFromWhere"`
	SubscriptionDatas []*SubscriptionData `json:"subscriptionDataSet"`
	UnitMode          bool                `json:"unitMode"`
}

func (c consumerData) UniqueID() string {
	return c.GroupName
}

type heartbeatData struct {
	ClientId      string    `json:"clientID"`
	ProducerDatas utils.Set `json:"producerDataSet"`
	ConsumerDatas utils.Set `json:"consumerDataSet"`
}

func NewHeartbeatData(clientID string) *heartbeatData {
	return &heartbeatData{
		ClientId:      clientID,
		ProducerDatas: utils.NewSet(),
		ConsumerDatas: utils.NewSet(),
	}
}

func (data *heartbeatData) encode() []byte {
	d, err := jsoniter.Marshal(data)
	if err != nil {
		rlog.Error("marshal heartbeatData error", map[string]interface{}{
			rlog.LogKeyUnderlayError: err,
		})
		return nil
	}
	rlog.Debug("heartbeat: "+string(d), nil)
	return d
}

const (
	PropNameServerAddr         = "PROP_NAMESERVER_ADDR"
	PropThreadPoolCoreSize     = "PROP_THREADPOOL_CORE_SIZE"
	PropConsumeOrderly         = "PROP_CONSUMEORDERLY"
	PropConsumeType            = "PROP_CONSUME_TYPE"
	PropClientVersion          = "PROP_CLIENT_VERSION"
	PropConsumerStartTimestamp = "PROP_CONSUMER_START_TIMESTAMP"
)

type ProcessQueueInfo struct {
	CommitOffset            int64 `json:"commitOffset"`
	CachedMsgMinOffset      int64 `json:"cachedMsgMinOffset"`
	CachedMsgMaxOffset      int64 `json:"cachedMsgMaxOffset"`
	CachedMsgCount          int   `json:"cachedMsgCount"`
	CachedMsgSizeInMiB      int64 `json:"cachedMsgSizeInMiB"`
	TransactionMsgMinOffset int64 `json:"transactionMsgMinOffset"`
	TransactionMsgMaxOffset int64 `json:"transactionMsgMaxOffset"`
	TransactionMsgCount     int   `json:"transactionMsgCount"`
	Locked                  bool  `json:"locked"`
	TryUnlockTimes          int64 `json:"tryUnlockTimes"`
	LastLockTimestamp       int64 `json:"lastLockTimestamp"`
	Dropped                 bool  `json:"dropped"`
	LastPullTimestamp       int64 `json:"lastPullTimestamp"`
	LastConsumeTimestamp    int64 `json:"lastConsumeTimestamp"`
}

type ConsumeStatus struct {
	PullRT            float64 `json:"pullRT"`
	PullTPS           float64 `json:"pullTPS"`
	ConsumeRT         float64 `json:"consumeRT"`
	ConsumeOKTPS      float64 `json:"consumeOKTPS"`
	ConsumeFailedTPS  float64 `json:"consumeFailedTPS"`
	ConsumeFailedMsgs int64   `json:"consumeFailedMsgs"`
}

type ConsumerRunningInfo struct {
	Properties       map[string]string
	SubscriptionData map[*SubscriptionData]bool
	MQTable          map[primitive.MessageQueue]ProcessQueueInfo
	StatusTable      map[string]ConsumeStatus
}

func (info ConsumerRunningInfo) Encode() ([]byte, error) {
	data, err := json.Marshal(info.Properties)
	if err != nil {
		return nil, err
	}
	jsonData := fmt.Sprintf("{\"%s\":%s", "properties", string(data))

	data, err = json.Marshal(info.StatusTable)
	if err != nil {
		return nil, err
	}
	jsonData = fmt.Sprintf("%s,\"%s\":%s", jsonData, "statusTable", string(data))

	subs := make([]*SubscriptionData, len(info.SubscriptionData))
	idx := 0
	for k := range info.SubscriptionData {
		subs[idx] = k
		idx++
	}

	// make sure test case table
	sort.Slice(subs, func(i, j int) bool {
		sub1 := subs[i]
		sub2 := subs[j]
		if sub1.ClassFilterMode != sub2.ClassFilterMode {
			return sub1.ClassFilterMode == false
		}
		com := strings.Compare(sub1.Topic, sub1.Topic)
		if com != 0 {
			return com > 0
		}

		com = strings.Compare(sub1.SubString, sub1.SubString)
		if com != 0 {
			return com > 0
		}

		if sub1.SubVersion != sub2.SubVersion {
			return sub1.SubVersion > sub2.SubVersion
		}

		com = strings.Compare(sub1.ExpType, sub1.ExpType)
		if com != 0 {
			return com > 0
		}

		v1, _ := sub1.Tags.MarshalJSON()
		v2, _ := sub2.Tags.MarshalJSON()
		com = bytes.Compare(v1, v2)
		if com != 0 {
			return com > 0
		}

		v1, _ = sub1.Codes.MarshalJSON()
		v2, _ = sub2.Codes.MarshalJSON()
		com = bytes.Compare(v1, v2)
		if com != 0 {
			return com > 0
		}
		return true
	})

	data, err = json.Marshal(subs)
	if err != nil {
		return nil, err
	}
	jsonData = fmt.Sprintf("%s,\"%s\":%s", jsonData, "subscriptionSet", string(data))

	tableJson := ""
	keys := make([]primitive.MessageQueue, 0)

	for k := range info.MQTable {
		keys = append(keys, k)
	}

	sort.Slice(keys, func(i, j int) bool {
		q1 := keys[i]
		q2 := keys[j]
		com := strings.Compare(q1.Topic, q2.Topic)
		if com != 0 {
			return com < 0
		}

		com = strings.Compare(q1.BrokerName, q2.BrokerName)
		if com != 0 {
			return com < 0
		}

		return q1.QueueId < q2.QueueId
	})

	for idx := range keys {
		dataK, err := json.Marshal(keys[idx])
		if err != nil {
			return nil, err
		}
		dataV, err := json.Marshal(info.MQTable[keys[idx]])
		tableJson = fmt.Sprintf("%s,%s:%s", tableJson, string(dataK), string(dataV))
	}
	tableJson = strings.TrimLeft(tableJson, ",")
	jsonData = fmt.Sprintf("%s,\"%s\":%s}", jsonData, "mqTable", fmt.Sprintf("{%s}", tableJson))
	return []byte(jsonData), nil
}

func NewConsumerRunningInfo() *ConsumerRunningInfo {
	return &ConsumerRunningInfo{
		Properties:       make(map[string]string),
		SubscriptionData: make(map[*SubscriptionData]bool),
		MQTable:          make(map[primitive.MessageQueue]ProcessQueueInfo),
		StatusTable:      make(map[string]ConsumeStatus),
	}
}
