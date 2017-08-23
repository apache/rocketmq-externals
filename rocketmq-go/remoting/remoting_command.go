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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"sync/atomic"
)

var opaque int32

//RESPONSE_TYPE RESPONSE_TYPE int= 1 << RPC_TYPE
var RESPONSE_TYPE int = 1

//RemotingCommand rocketmq remoting command
// both request and response use it
type RemotingCommand struct {
	//request:  request_code.go
	//response: response_code.go
	Code int16 `json:"code"`
	//this client's language. see config.go
	Language string `json:"language"` //int 8
	//this client's version. see config.go
	Version int16 `json:"version"`
	//the client's Opaque,it is auto increase
	Opaque int32 `json:"opaque"`
	//this request's flag
	Flag int `json:"flag"`
	//remark, for example error message
	Remark string `json:"remark"`
	//this request's param
	ExtFields map[string]interface{} `json:"extFields"` //java's ExtFields and customHeader is use this key word
	//response content
	Body []byte `json:"body,omitempty"`
}

//NewRemotingCommand NewRemotingCommand
func NewRemotingCommand(commandCode int16, customerHeader CustomerHeader) *RemotingCommand {
	return NewRemotingCommandWithBody(commandCode, customerHeader, nil)
}

//NewRemotingCommandWithBody NewRemotingCommandWithBody
func NewRemotingCommandWithBody(commandCode int16, customerHeader CustomerHeader, body []byte) *RemotingCommand {
	remotingCommand := new(RemotingCommand)
	remotingCommand.Code = commandCode
	currOpaque := atomic.AddInt32(&opaque, 1)
	remotingCommand.Opaque = currOpaque
	remotingCommand.Flag = constant.REMOTING_COMMAND_FLAG
	remotingCommand.Language = constant.REMOTING_COMMAND_LANGUAGE
	remotingCommand.Version = constant.REMOTING_COMMAND_VERSION
	if customerHeader != nil {
		remotingCommand.ExtFields = util.Struct2Map(customerHeader)
	}
	remotingCommand.Body = body
	return remotingCommand
}

func (r *RemotingCommand) isResponseType() bool {
	return r.Flag&(RESPONSE_TYPE) == RESPONSE_TYPE
}
func (r *RemotingCommand) markResponseType() {
	r.Flag = (r.Flag | RESPONSE_TYPE)
}
