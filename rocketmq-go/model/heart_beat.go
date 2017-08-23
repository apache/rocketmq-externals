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

package model

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"

//ConsumerData this client's ConsumerData
type ConsumerData struct {
	GroupName           string
	ConsumeType         string
	MessageModel        string
	ConsumeFromWhere    rocketmqm.ConsumeFromWhere
	SubscriptionDataSet []*SubscriptionData
	UnitMode            bool
}

//ProducerData producer data
type ProducerData struct {
	GroupName string
}

//HeartbeatData heartbeat data
type HeartbeatData struct {
	ClientId        string
	ConsumerDataSet []*ConsumerData
	ProducerDataSet []*ProducerData
}
