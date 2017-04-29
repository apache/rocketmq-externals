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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"os"
	"strconv"
	"time"
)

func init() {
	os.Setenv(remoting.RemotingVersionKey, strconv.Itoa(rocketmq.CurrentVersion))
}

//sendSmartMsg, _ := strconv.ParseBool(os.Getenv("org.apache.rocketmq.client.sendSmartMsg"))

type TopAddress struct {
}

type ClientRemotingProcessor interface {
}

type MQClientAPI struct {
	rClient           *remoting.RemotingClient
	topAddress        *TopAddress
	crp               *ClientRemotingProcessor
	nameServerAddress string
	config            *config.ClientConfig
}

// TODO unfinished
func NewMQClientAPI(cfg *config.ClientConfig, processor *ClientRemotingProcessor, hook remoting.RPCHook) *MQClientAPI {
	api := &MQClientAPI{
		rClient:    &remoting.RemotingClient{}, //TODO
		topAddress: &TopAddress{},              // TODO
		crp:        processor,
		config:     cfg,
	}

	// TODO register
	return api
}

func (api *MQClientAPI) NameServerAddressList() []string
func (api *MQClientAPI) FetchNameServerAddress() string
func (api *MQClientAPI) UpdateNameServerAddressList(ads []string) {}
func (api *MQClientAPI) Start()
func (api *MQClientAPI) Shutdown()

type TopicConfig struct {
	// TODO
}

func (api *MQClientAPI) CreateTopic(address, defaultTopic string, cfg TopicConfig, timeout time.Duration)

func (api *MQClientAPI) SendMessage(addr, brokerName string,
	msg message.Message, requestHeader header.SendMessageRequestHeader, timeout int64) *model.SendResult {
	var request *remoting.RemotingCommand
	request = remoting.CreateRemotingCommand(model.SendMsg, &requestHeader)
	request.SetBody(msg.Body)
	return api.sendMessageSync(addr, brokerName, msg, timeout, request)
}

func (api *MQClientAPI) sendMessageSync(addr, brokerName string,
	msg message.Message,
	timeout int64,
	request *remoting.RemotingCommand) *model.SendResult {

	return nil
	// TODO return api.processSendResponse(brokerName, msg, response)
}

func (api *MQClientAPI) onErrorDo( /* TODO*/ )

func (api *MQClientAPI) processSendResponse(name string, msg message.Message, cmd *remoting.RemotingCommand) *remoting.RemotingCommand {
	return nil
}
func (api *MQClientAPI) PullMessage(address string,
	requestHeader header.PullMessageRequestHeader,
	timeout time.Duration,
	mode remoting.CommunicationMode,
	callback model.PullCallback) (model.PullResult, error)
func (api *MQClientAPI) pullMessageAsync(address string,
	request remoting.RemotingCommand,
	timeout time.Duration,
	callback model.PullCallback)

func (api *MQClientAPI) pullMessageSync(address string,
	request remoting.RemotingCommand,
	timeout time.Duration) (model.PullResult, error)
func (api *MQClientAPI) processPullResponse(response remoting.RemotingCommand) (model.PullResult, error)

type HeartbeatData struct {
	// TODO
}

func (api *MQClientAPI) SendHeartBeat(address string, hbd HeartbeatData, timeout time.Duration)
func (api *MQClientAPI) ConsumerSendMessageBack(address, consumerGroup string, msgX message.MessageExt, delayLevel, retryTimes int, timeout time.Duration)
func (api *MQClientAPI) TopicRouteInfoFromNameServer(topic string, timeout time.Duration) model.TopicRouteData

func (api *MQClientAPI) RegisterMessageFilterClass(consumerGroup, topic, className string, classCRC int, classBody []byte, timeout time.Duration) error
