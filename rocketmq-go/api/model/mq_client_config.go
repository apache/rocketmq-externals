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

package rocketmqm

//SerializeType default serialize type is JSON_SERIALIZE, but ROCKETMQ_SERIALIZE(need version >= ?) is faster
type SerializeType byte

const (
	//JSON_SERIALIZE see json_serializable.go
	JSON_SERIALIZE SerializeType = iota
	//ROCKETMQ_SERIALIZE see rocketmq_serializable.go
	ROCKETMQ_SERIALIZE
)

//MqClientConfig MqClientConfig
type MqClientConfig struct {
	// NameServerAddress split by ;
	NameServerAddress string
	//this client use which serialize type
	ClientSerializeType SerializeType
}

//NewMqClientConfig create a MqClientConfig instance
func NewMqClientConfig(nameServerAddress string) (mqClientConfig *MqClientConfig) {
	mqClientConfig = &MqClientConfig{
		NameServerAddress:   nameServerAddress,
		ClientSerializeType: JSON_SERIALIZE,
	}
	return
}
