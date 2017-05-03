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
package consumer

import "time"

type RocketMqConsumerConfig struct {
	// pull
	consumerGroup string
	brokerSuspendMaxTime time.Duration
	consumerTimeoutMillisWhenSuspend time.Duration
	consumerPullTimeout time.Duration
	messageModel MessageModel
	unitMode bool
	maxReconsumeTimes int
}

func (cfg *RocketMqConsumerConfig) BuildConsumerConfig(file string) RocketMqConsumerConfig {
	return RocketMqConsumerConfig{
		consumerGroup: "default",
		brokerSuspendMaxTime: 20 * time.Second,
		consumerTimeoutMillisWhenSuspend: 30 * time.Second,
		consumerPullTimeout: 10 * time.Second,
		messageModel: Clustering,
		unitMode: false,
		maxReconsumeTimes: 16,
	}
}

func (cfg *RocketMqConsumerConfig) SetConsumerGroup(group string) {
	cfg.consumerGroup = group
}
