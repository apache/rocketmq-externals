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

package rocketmqm

//MqProducerConfig MqProducerConfig
type MqProducerConfig struct {
	// SendMsgTimeout for this producer
	SendMsgTimeout int64
	// CompressMsgBodyOverHowMuch
	CompressMsgBodyOverHowMuch int
	// ZipCompressLevel
	ZipCompressLevel                 int
	RetryTimesWhenSendFailed         int
	RetryTimesWhenSendAsyncFailed    int
	RetryAnotherBrokerWhenNotStoreOK bool
	MaxMessageSize                   int
	SendLatencyFaultEnable           bool
	LatencyMax                       []int64
	NotAvailableDuration             []int64
}

//NewProducerConfig create a MqProducerConfig instance
func NewProducerConfig() (producerConfig *MqProducerConfig) {
	producerConfig = &MqProducerConfig{
		SendMsgTimeout:             3000,
		CompressMsgBodyOverHowMuch: 1024 * 4,
		ZipCompressLevel:           5,
		MaxMessageSize:             1024 * 1024 * 4, // 4M

		RetryTimesWhenSendFailed:         2,
		RetryTimesWhenSendAsyncFailed:    2,
		RetryAnotherBrokerWhenNotStoreOK: false,
		SendLatencyFaultEnable:           false,
		LatencyMax:                       []int64{50, 100, 550, 1000, 2000, 3000, 15000},
		NotAvailableDuration:             []int64{0, 0, 30000, 60000, 120000, 180000, 600000},
	}
	return
}
