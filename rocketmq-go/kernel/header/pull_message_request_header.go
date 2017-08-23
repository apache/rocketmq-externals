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

//PullMessageRequestHeader of CustomerHeader
type PullMessageRequestHeader struct {
	ConsumerGroup        string `json:"consumerGroup"`
	Topic                string `json:"topic"`
	QueueId              int32  `json:"queueId"`
	QueueOffset          int64  `json:"queueOffset"`
	MaxMsgNums           int32  `json:"maxMsgNums"`
	SysFlag              int32  `json:"sysFlag"`
	CommitOffset         int64  `json:"commitOffset"`
	SuspendTimeoutMillis int64  `json:"suspendTimeoutMillis"`
	Subscription         string `json:"subscription"`
	SubVersion           int64  `json:"subVersion"`
}

//FromMap convert map[string]interface to struct
func (p *PullMessageRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}
