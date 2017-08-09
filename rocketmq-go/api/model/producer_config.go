/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package rocketmq_api_model

type RocketMqProducerConfig struct {
	SendMsgTimeout int64 //done
	//private int sendMsgTimeout = 3000;
	CompressMsgBodyOverHowMuch int //done
	//private int compressMsgBodyOverHowmuch = 1024 * 4;
	ZipCompressLevel int //done
	//private int zipCompressLevel = Integer.parseInt(System.getProperty(MixAll.MESSAGE_COMPRESS_LEVEL, "5"));
	/**
	 * Just for testing or demo program
	 */
	//	private String createTopicKey = MixAll.DEFAULT_TOPIC;

	//DefaultTopicQueueNums            int
	////private volatile int defaultTopicQueueNums = 4;

	RetryTimesWhenSendFailed int
	//private int retryTimesWhenSendFailed = 2;
	RetryTimesWhenSendAsyncFailed int
	//private int retryTimesWhenSendAsyncFailed = 2;
	//
	RetryAnotherBrokerWhenNotStoreOK bool
	//private boolean retryAnotherBrokerWhenNotStoreOK = false;
	MaxMessageSize int
	//private int maxMessageSize = 1024 * 1024 * 4; // 4M

	//for MQFaultStrategy todo to be done
	SendLatencyFaultEnable bool    //false
	LatencyMax             []int64 //=             {50L,   100L,   550L,       1000L,  2000L,      3000L,      15000L};
	NotAvailableDuration   []int64 //   {0L,    0L,     30000L,     60000L, 120000L,    180000L,    600000L};
}

//set defaultValue
func NewProducerConfig() (producerConfig *RocketMqProducerConfig) {
	producerConfig = &RocketMqProducerConfig{
		SendMsgTimeout:             3000,
		CompressMsgBodyOverHowMuch: 1024 * 4,
		ZipCompressLevel:           5,
		MaxMessageSize:             1024 * 1024 * 4, // 4M

		RetryTimesWhenSendFailed:         2,
		RetryTimesWhenSendAsyncFailed:    2, //
		RetryAnotherBrokerWhenNotStoreOK: false,
		SendLatencyFaultEnable:           false,
		LatencyMax:                       []int64{50, 100, 550, 1000, 2000, 3000, 15000},
		NotAvailableDuration:             []int64{0, 0, 30000, 60000, 120000, 180000, 600000},
	}
	return
}
