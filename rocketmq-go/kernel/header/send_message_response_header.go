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

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

//SendMessageResponseHeader of CustomerHeader
type SendMessageResponseHeader struct {
	MsgId         string
	QueueId       int32
	QueueOffset   int64
	TransactionId string
	MsgRegion     string
}

//FromMap convert map[string]interface to struct
func (s *SendMessageResponseHeader) FromMap(headerMap map[string]interface{}) {
	s.MsgId = headerMap["msgId"].(string)
	s.QueueId = util.StrToInt32WithDefaultValue(headerMap["queueId"].(string), -1)
	s.QueueOffset = util.StrToInt64WithDefaultValue(headerMap["queueOffset"].(string), -1)
	transactionId := headerMap["transactionId"]
	if transactionId != nil {
		s.TransactionId = headerMap["transactionId"].(string)
	}
	msgRegion := headerMap["MSG_REGION"]
	if msgRegion != nil {
		s.MsgRegion = headerMap["MSG_REGION"].(string)
	}

	return
}

//for example

/**
{
    "MSG_REGION": "DefaultRegion",
    "TRACE_ON": "true",
    "msgId": "C0A8000200002A9F0000000039FA93B5",
    "queueId": "3",
    "queueOffset": "1254671"
}
*/
