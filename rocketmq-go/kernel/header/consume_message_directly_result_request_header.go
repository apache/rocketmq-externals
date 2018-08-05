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

//ConsumeMessageDirectlyResultRequestHeader of CustomerHeader
type ConsumeMessageDirectlyResultRequestHeader struct {
	//ConsumerGroup
	ConsumerGroup string `json:"consumerGroup"`
	//ClientId
	ClientId string `json:"clientId"`
	//MsgId
	MsgId string `json:"msgId"`
	//BrokerName
	BrokerName string `json:"brokerName"`
}

//FromMap convert map[string]interface to struct
func (c *ConsumeMessageDirectlyResultRequestHeader) FromMap(headerMap map[string]interface{}) {
	c.ConsumerGroup = headerMap["consumerGroup"].(string)
	c.ClientId = headerMap["clientId"].(string)
	c.MsgId = headerMap["msgId"].(string)
	c.BrokerName = headerMap["brokerName"].(string)
	return
}
