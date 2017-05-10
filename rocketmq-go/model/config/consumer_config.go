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
package config

import "time"

/**
 * Delay some time when exception occur
 */
const PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION int64 = 3000

/**
 * Flow control interval
 */
const PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL int64 = 50

//consume from where
//first consume from the last offset
const CONSUME_FROM_LAST_OFFSET string = "CONSUME_FROM_LAST_OFFSET"

//first consume from the first offset
const CONSUME_FROM_FIRST_OFFSET string = "CONSUME_FROM_FIRST_OFFSET"

//first consume from the time
const CONSUME_FROM_TIMESTAMP string = "CONSUME_FROM_TIMESTAMP"

//consume from where

type RocketMqConsumerConfig struct {
	ConsumeFromWhere string
	/**
	 * Minimum consumer thread number
	 */
	//consumeThreadMin                  int
	//					/**
	//					 * Max consumer thread number
	//					 */
	//consumeThreadMax                  int

	/**
	 * Threshold for dynamic adjustment of the number of thread pool
	 */
	//adjustThreadPoolNumsThreshold     int   // = 100000;

	/**
	 * Concurrently max span offset.it has no effect on sequential consumption
	 */
	ConsumeConcurrentlyMaxSpan int // = 2000;
	/**
	 * Flow control threshold
	 */
	PullThresholdForQueue int //= 1000;
	/**
	 * Message pull Interval
	 */
	PullInterval int64 //= 0;
	/**
	 * Batch consumption size
	 */
	ConsumeMessageBatchMaxSize int //= 1;
	/**
	 * Batch pull size
	 */
	PullBatchSize int //= 32;

	/**
	 * Whether update subscription relationship when every pull
	 */
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

func NewRocketMqConsumerConfig() (consumerConfig *RocketMqConsumerConfig) {
	consumerConfig = &RocketMqConsumerConfig{
		ConsumeFromWhere:              CONSUME_FROM_LAST_OFFSET,          //3种都可以选择
		ConsumeConcurrentlyMaxSpan:    2000,                              //流量控制 就是队列里最大的offset和最小的offset差值比较
		PullThresholdForQueue:         1000,                              //最大的存放消息的量 msgCount
		PullInterval:                  0,                                 //拉消息等待的时间间隔 毫秒数
		ConsumeMessageBatchMaxSize:    1,                                 //每次消费的msgList最大长度
		PullBatchSize:                 32,                                //每次从broker拉的最大消息条数
		PostSubscriptionWhenPull:      false,                             //no use 没啥用
		UnitMode:                      false,                             //no use 没啥用
		MaxReconsumeTimes:             16,                                // 最多重试次数 对于3.4.9+的broker 这个在console那边配置没有用了
		SuspendCurrentQueueTimeMillis: 1000,                              // 没啥用 顺序消费的会使用它
		ConsumeTimeout:                15,                                //消费超时 单位是分钟 一个消息要是超过这个时间还没消费掉 那么会被扔出去
		ConsumeTimestamp:              time.Now().Add(-30 * time.Minute), //你要是选择了CONSUME_FROM_TIMESTAMP 那么需要配置这个时间

		// use custom or constants.don't suggest to change 不太建议普通用户去改它
		PullTimeDelayMillsWhenException:   PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION,    //拉消息发生异常 等待多少时间
		PullTimeDelayMillsWhenFlowControl: PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL, //拉消息遇到了流量控制 等待多少时间
		PullTimeDelayMillsWhenSuspend:     1000,                                    // no use 没啥用
		BrokerSuspendMaxTimeMillis:        1000 * 15,                               //没有新消息 broker挂起多少时间
		ConsumerTimeoutMillisWhenSuspend:  1000 * 30,                               //没啥用

	}
	return
}
