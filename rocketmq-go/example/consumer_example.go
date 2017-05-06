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
package main

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
)

func main() {

	var clienConfig = config.NewClientConfig()
	clienConfig.SetNameServerAddress("120.55.113.35:9876")

	//use json serializer
	var mqClient = service.MqClientInit(clienConfig, nil)
	fmt.Println(mqClient.TryToFindTopicPublishInfo("GoLang"))
	//&{false true [{GoLang broker-a 0} {GoLang broker-a 1} {GoLang broker-a 2} {GoLang broker-a 3}] 0xc420016800 0} <nil>

	//use rocketmq serializer
	constant.USE_HEADER_SERIALIZETYPE = constant.ROCKETMQ_SERIALIZE
	var mqClient2 = service.MqClientInit(clienConfig, nil)
	fmt.Println(mqClient2.TryToFindTopicPublishInfo("GoLang"))
}
