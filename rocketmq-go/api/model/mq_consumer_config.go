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

import "time"

//PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION Delay some time when exception occur
const PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION int64 = 3000

//PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL Flow control interval
const PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL int64 = 50

//ConsumeFromWhere consume from where
type ConsumeFromWhere int

//first consume from the last offset
const (
	//CONSUME_FROM_LAST_OFFSET first consume from the last offset
	CONSUME_FROM_LAST_OFFSET ConsumeFromWhere = iota

	//CONSUME_FROM_FIRST_OFFSET first consume from the first offset
	CONSUME_FROM_FIRST_OFFSET

	//CONSUME_FROM_TIMESTAMP first consume from the time
	CONSUME_FROM_TIMESTAMP
)

//MqConsumerConfig MqConsumerConfig
type MqConsumerConfig struct {

	//consume from where
	ConsumeFromWhere ConsumeFromWhere

	//Concurrently max span offset.it has no effect on sequential consumption
	ConsumeConcurrentlyMaxSpan int // = 2000;

	//Flow control threshold
	PullThresholdForQueue int //= 1000;

	//Message pull Interval
	PullInterval int64 //= 0;

	//Batch consumption size
	ConsumeMessageBatchMaxSize int //= 1;

	//Batch pull size
	PullBatchSize int //= 32;

	//Whether update subscription relationship when every pull
	PostSubscriptionWhenPull bool //= false; //get subExpression

	/**
	 * Whether the unit of subscription group
	 */
	UnitMode                      bool  // = false;
	MaxReconsumeTimes             int   //= 16;
	SuspendCurrentQueueTimeMillis int64 //= 1000;
	ConsumeTimeout                int64 //= 15 //minutes

	//=========can not change
	/**
	 * Delay some time when exception occur
	 */
	PullTimeDelayMillsWhenException int64 //= 3000;
	/**
	 * Flow control interval
	 */
	PullTimeDelayMillsWhenFlowControl int64 //= 50;
	/**
	 * Delay some time when suspend pull service
	 */
	PullTimeDelayMillsWhenSuspend    int64 //= 1000;
	BrokerSuspendMaxTimeMillis       int64 //1000 * 15;
	ConsumerTimeoutMillisWhenSuspend int64 //= 1000 * 30;

	/**
	* Backtracking consumption time with second precision.time format is
	* 20131223171201
	* Implying Seventeen twelve and 01 seconds on December 23, 2013 year
	* Default backtracking consumption time Half an hour ago
	 */
	ConsumeTimestamp time.Time //when use CONSUME_FROM_TIMESTAMP
}

//NewRocketMqConsumerConfig create a MqConsumerConfig instance
func NewRocketMqConsumerConfig() (consumerConfig *MqConsumerConfig) {
	consumerConfig = &MqConsumerConfig{
		ConsumeFromWhere:              CONSUME_FROM_LAST_OFFSET,
		ConsumeConcurrentlyMaxSpan:    2000,
		PullThresholdForQueue:         1000,
		PullInterval:                  0,
		ConsumeMessageBatchMaxSize:    1,
		PullBatchSize:                 32,
		PostSubscriptionWhenPull:      false,
		UnitMode:                      false,
		MaxReconsumeTimes:             16,
		SuspendCurrentQueueTimeMillis: 1000,
		ConsumeTimeout:                15,
		ConsumeTimestamp:              time.Now().Add(-30 * time.Minute),

		// use custom or constants.don't suggest to change
		PullTimeDelayMillsWhenException:   PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION,
		PullTimeDelayMillsWhenFlowControl: PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL,
		PullTimeDelayMillsWhenSuspend:     1000,
		BrokerSuspendMaxTimeMillis:        1000 * 15,
		ConsumerTimeoutMillisWhenSuspend:  1000 * 30,
	}
	return
}
