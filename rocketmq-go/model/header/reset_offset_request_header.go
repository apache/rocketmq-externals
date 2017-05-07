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

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"strconv"
)

type ResetOffsetRequestHeader struct {
	Topic     string `json:"topic"`
	Group     string `json:"group"`
	Timestamp int64  `json:"timestamp"`
	IsForce   bool   `json:"isForce"`
}

func (self *ResetOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.Group = headerMap["group"].(string)
	self.Topic = headerMap["topic"].(string)
	self.Timestamp = util.StrToInt64WithDefaultValue(headerMap["timestamp"].(string), -1)
	self.IsForce, _ = strconv.ParseBool(headerMap["isForce"].(string))
	return
}
