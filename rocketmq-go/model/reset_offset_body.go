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
package model

import (
	"encoding/json"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
)

type ResetOffsetBody struct {
	OffsetTable map[MessageQueue]int64 `json:"offsetTable"`
}

func (self *ResetOffsetBody) Decode(data []byte) (err error) {
	self.OffsetTable = map[MessageQueue]int64{}
	var kvMap map[string]string
	kvMap, err = util.GetKvStringMap(string(data))
	if err != nil {
		return
	}
	glog.Info(kvMap)
	kvMap, err = util.GetKvStringMap(kvMap["\"offsetTable\""])
	if err != nil {
		return
	}
	for k, v := range kvMap {
		messageQueue := &MessageQueue{}
		var offset int64
		err = json.Unmarshal([]byte(k), messageQueue)
		if err != nil {
			return
		}
		offset, err = util.StrToInt64(v)
		if err != nil {
			return
		}
		self.OffsetTable[*messageQueue] = offset
	}
	return
}
