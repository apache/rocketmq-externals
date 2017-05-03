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

package service

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
)

var sendSmartMsg bool = true // TODO get from system env

type TopAddress struct {
}

type ClientRemotingProcessor interface {
}

func init() {
	// TODO
}

type MQClientAPI struct {
	remotingClient    *remoting.RemotingClient
	topAddress        *TopAddress
	crp               *ClientRemotingProcessor
	nameServerAddress string
	config            *config.ClientConfig
}

//func NewMQClientAPI(cfg *config.ClientConfig, processor *ClientRemotingProcessor, hook remoting.RPCHook) *MQClientAPI {
//	api := &MQClientAPI{
//		remotingClient: &remoting.RemotingClient{}, //TODO
//		topAddress:     &TopAddress{},              // TODO
//		crp:            processor,
//		config:         cfg,
//	}
//
//	// TODO register
//	return api
//}
//
//func (api *MQClientAPI) SendMessage(addr, brokerName string,
//	msg message.Message, requestHeader header.SendMessageRequestHeader, timeout int64) *model.SendResult {
//	var request *remoting.RemotingCommand
//	request = remoting.CreateRemotingCommand(model.SendMsg, &requestHeader)
//	request.SetBody(msg.Body)
//	return api.sendMessageSync(addr, brokerName, msg, timeout, request)
//}

func (api *MQClientAPI) sendMessageSync(addr, brokerName string,
	msg message.Message,
	timeout int64,
	request *remoting.RemotingCommand) *model.SendResult {
	response := api.invokeSync(addr, request, timeout)
	if response == nil {
		panic("invokeSync panci!")
	}
	return nil
	// TODO return api.processSendResponse(brokerName, msg, response)
}

func (api *MQClientAPI) invokeSync(addr string, cmd *remoting.RemotingCommand, timeout int64) *remoting.RemotingCommand {
	return nil
}

func (api *MQClientAPI) processSendResponse(name string, msg message.Message, cmd *remoting.RemotingCommand) *remoting.RemotingCommand {
	return nil
}
