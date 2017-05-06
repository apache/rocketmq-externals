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
package header

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

type UpdateConsumerOffsetRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
	Topic         string `json:"topic"`
	QueueId       int32  `json:"queueId"`
	CommitOffset  int64  `json:"commitOffset"`
}

func (self *UpdateConsumerOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.ConsumerGroup = headerMap["consumerGroup"].(string)
	self.QueueId = util.StrToInt32WithDefaultValue(util.ReadString(headerMap["queueId"]), 0)
	self.CommitOffset = util.StrToInt64WithDefaultValue(headerMap["commitOffset"].(string), -1)
	self.Topic = util.ReadString(headerMap["topic"])
	return
}
