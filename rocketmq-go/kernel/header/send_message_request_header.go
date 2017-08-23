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

package header

//SendMessageRequestHeader of CustomerHeader
type SendMessageRequestHeader struct {
	ProducerGroup         string `json:"producerGroup"`
	Topic                 string `json:"topic"`
	DefaultTopic          string `json:"defaultTopic"`
	DefaultTopicQueueNums int    `json:"defaultTopicQueueNums"`
	QueueId               int32  `json:"queueId"`
	SysFlag               int    `json:"sysFlag"`
	BornTimestamp         int64  `json:"bornTimestamp"`
	Flag                  int    `json:"flag"`
	Properties            string `json:"properties"`
	ReconsumeTimes        int    `json:"reconsumeTimes"`
	UnitMode              bool   `json:"unitMode"`
	MaxReconsumeTimes     int    `json:"maxReconsumeTimes"`
}

//FromMap to SendMessageRequestHeader
func (s *SendMessageRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}
