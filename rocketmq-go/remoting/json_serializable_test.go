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

package remoting

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"testing"
)

var testJson = "{\"code\":1,\"language\":\"GO\",\"version\":1,\"opaque\":1,\"flag\":1,\"remark\":\"remark\",\"extFields\":{\"key1\":\"str\",\"key2\":1},\"body\":\"AQIDBA==\"}"

func TestEncodeHeaderData(t *testing.T) {
	testMap := make(map[string]interface{})
	testMap["key1"] = "str"
	testMap["key2"] = 1
	command := &remoting.RemotingCommand{
		Code:      1,
		Language:  "GO",
		Version:   1,
		Opaque:    1,
		Flag:      1,
		Remark:    "remark",
		ExtFields: testMap,
		Body:      []byte{1, 2, 3, 4},
	}
	jsonSerializer := remoting.JsonSerializer{}

	resultJson := jsonSerializer.encodeHeaderData(command)
	if testJson != string(resultJson) {
		t.Errorf("resultJson is not equals testJson resultJson=%s ", resultJson)
	}
}

func TestDecodeRemoteCommand(t *testing.T) {
	jsonSerializer := remoting.JsonSerializer{}
	testByte := []byte(testJson)
	remotingCommand := jsonSerializer.decodeRemoteCommand(testByte, []byte{1, 2, 3, 4})
	if remotingCommand.Language != "GO" || remotingCommand.Remark != "remark" {
		t.Error("TestDecodeRemoteCommand fail reslutData")
	} else {
		t.Log("TestDecodeRemoteCommandSuccess")
	}
}
