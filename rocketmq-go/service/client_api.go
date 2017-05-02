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
	"github.com/golang/glog"
	"errors"
	"fmt"
)

func init() {
	os.Setenv(remoting.RemotingVersionKey, strconv.Itoa(rocketmq.CurrentVersion))
}

var sendSmartMsg bool = false // TODO, _ := strconv.ParseBool(os.Getenv("org.apache.rocketmq.client.sendSmartMsg"))

type TopAddress struct {
}

type ClientRemotingProcessor interface {
}

type MQClientAPI struct {
	rClient           *remoting.RemotingClient
	topAddress        *TopAddress
	crp               *remoting.ClientRemotingProcessor
	nameServerAddress string
	config            *config.ClientConfig
}

// TODO unfinished
func NewMQClientAPI(cfg *config.ClientConfig, processor *remoting.ClientRemotingProcessor, hook remoting.RPCHook) *MQClientAPI {
	api := &MQClientAPI{
		rClient:    &remoting.RemotingClient{}, //TODO
		topAddress: &TopAddress{},              // TODO
		crp:        processor,
		config:     cfg,
	}

	api.rClient.RegisterRPCHook(hook)
	// TODO modify signature
	api.rClient.RegisterProcessor(model.CheckTransactionState, processor, remoting.ExecutorService{})
	api.rClient.RegisterProcessor(model.NotifyConsumerIdsChanged, processor, remoting.ExecutorService{})
	api.rClient.RegisterProcessor(model.ResetConsumerClientOffset, processor, remoting.ExecutorService{})
	api.rClient.RegisterProcessor(model.GetConsumerStatusFromClient, processor, remoting.ExecutorService{})
	api.rClient.RegisterProcessor(model.GetConsumerRunningInfo, processor, remoting.ExecutorService{})
	api.rClient.RegisterProcessor(model.ConsumeMsgDirectly, processor, remoting.ExecutorService{})

	return api
}

func (api *MQClientAPI) NameServerAddressList() []string {
	return api.rClient.NameServerAddressList()
}

func (api *MQClientAPI) FetchNameServerAddress() string {
	//ads := api.topAddress.
	return nil
}

func (api *MQClientAPI) UpdateNameServerAddressList(ads []string) {
	api.rClient.UpdateNameServerAddressList(ads)
}

func (api *MQClientAPI) Start() {
	api.rClient.Start()
}
func (api *MQClientAPI) Shutdown() {
	api.rClient.Shutdown()
}

type TopicConfig struct {
	// TODO
}

func (api *MQClientAPI) CreateTopic(address, defaultTopic string, cfg TopicConfig, timeout time.Duration) error {
	requestHeader := header.NewCreateTopicRequestHeader() // TODO
	request := remoting.CreateRemotingCommand(model.UpdateAndCreateTopic, requestHeader)
	// optimize
	response, err := api.rClient.InvokeSync(address, request, timeout)
	if err != nil {
		glog.Errorf("Create Topic %s ERROR: %s!", address, err.Error())
		return err
	}

	if response.Code != model.Success {
		return errors.New(fmt.Sprintf("Create Topic Failed, Response is: %v, Remark is %v",
			response.Code, response.Remark))
	}
	return nil
}


func (api *MQClientAPI) SendMessage(address, brokerName string,
	msg message.Message, requestHeader header.SendMessageRequestHeader, timeout time.Duration,
	mode remoting.CommunicationMode, callback model.SendCallback, topicInfo model.TopicPublishInfo,
	retryTimesWhenFailed int, ctx model.SendMessageContext) (*model.SendResult, error) {
	var request *remoting.RemotingCommand
	if sendSmartMsg {
		// TODO Send With V2
	}
	request = remoting.CreateRemotingCommand(model.SendMsg, &requestHeader)
	request.SetBody(msg.Body)
	switch mode {
	case remoting.OneWay:
		return nil, api.rClient.InvokeOneWay(address, request, timeout)
	case remoting.Async:
		// TODO
		return nil, nil
	case remoting.Sync:
		return api.sendMessageSync(address, brokerName, msg, timeout, request)
	default:
		glog.Fatalf("Illegal CommunicationMode %v", mode)
		return nil, errors.New(fmt.Sprintf("Illegal CommunicationMode %v", mode))
	}
}

func (api *MQClientAPI) sendMessageSync(address, brokerName string,
	msg message.Message,
	timeout time.Duration,
	request *remoting.RemotingCommand) (*model.SendResult, error) {
	response, err := api.rClient.InvokeSync(address, request, timeout)
	if err != nil {
		// TODO
	}

	return api.processSendResponse(brokerName, msg, response)
}
func (api *MQClientAPI) sendMessageAsync() // TODO
func (api *MQClientAPI) onErrorDo( /* TODO*/ )

func (api *MQClientAPI) processSendResponse(brokerName string, msg message.Message, response *remoting.RemotingCommand) (*model.SendResult, error) {
	switch response.Code {
	case model.FlushDiskTimeout:
		glog.Warningf("FlushDiskTimeout.")
	case model.FlushSlaveTimeout:
		glog.Warningf("FlushSlaveTimeout")
	case model.SlaveNotAvailable:
		glog.Warningf("SlaveNotAvailable")
	case model.Success:
		var responseHeader header.SendMessageResponseHeader // TODO decodeHeader
		messageQueue := message.NewMessageQueue(msg.Topic, brokerName, responseHeader.QueueId)
		sendResult := model.NewSendResult(model.SendOK, "TODO", responseHeader.MsgId, messageQueue, responseHeader.QueueOffset)
		sendResult.SetTransactionID(responseHeader.TransactionId)
		regionID, found := response.ExtFields[message.MessageConst.PropertyMsgRegion]
		if !found || regionID == "" {
			regionID = "DefaultRegion"
		}
		sendResult.SetRegionID(regionID)
		return sendResult, nil
	}

	return nil, model.NewMQBrokerError(response.Code, response.Remark)
}

// TODO 简化API
func (api *MQClientAPI) PullMessage(address string,
	requestHeader header.PullMessageRequestHeader,
	timeout time.Duration,
	mode remoting.CommunicationMode,
	callback model.PullCallback) (model.PullResult, error) {
	request := remoting.CreateRemotingCommand(model.PullMsg, requestHeader)
	switch mode {
	case remoting.Sync:
		return api.pullMessageSync(address, request, timeout)
	case remoting.Async:
		return api.pullMessageAsync(address, request, timeout, callback)
	default:
		glog.Errorf("Unexcepet CommunicationMode: %v", mode)
	}
	return nil, errors.New(fmt.Sprintf("Unexcepet CommunicationMode: %v", mode))
}

func (api *MQClientAPI) pullMessageSync(address string,
	request *remoting.RemotingCommand,
	timeout time.Duration) (model.PullResult, error) {
	response, err := api.rClient.InvokeSync(address, request, timeout)

	if err != nil {
		glog.Errorf("InvokeSync Error: %s", err.Error())
	}

	return api.processPullResponse(response)
}

func (api *MQClientAPI) pullMessageAsync(address string,
	request *remoting.RemotingCommand,
	timeout time.Duration,
	callback model.PullCallback) (model.PullResult, error) {
	// TODO
	return nil, nil
}

func (api *MQClientAPI) processPullResponse(response *remoting.RemotingCommand) (model.PullResult, error) {
	pullStatus := model.NoNewMsg
	var err error

	switch response.Code {
	case model.Success:
		pullStatus = model.Found
	case model.PullNotFound:
		pullStatus = model.NoNewMsg
	case model.PullRetryImmediately:
		pullStatus = model.NoMatchedMsg
	case model.PullOffsetMoved:
		pullStatus = model.OffsetIllegal
	default:
		err = model.NewMQBrokerError(response.Code, response.Remark)
	}
	//responseHeader := response.dec TODO decodeCommandCustomHeader
	var rh header.PullMessageResponseHeader
	pre := model.NewPullResultExt(pullStatus, rh.NextBeginOffset, rh.MinOffset, rh.MaxOffset,
		rh.SuggestWhichBrokerID, nil,  response.Body)
	return pre.PullResult, err // TODO PullResultExt necessary ？
}

type HeartbeatData struct {
	// TODO
}

func (api *MQClientAPI) SendHeartBeat(address string, hbd HeartbeatData, timeout time.Duration)
func (api *MQClientAPI) ConsumerSendMessageBack(address, consumerGroup string, msgX message.MessageExt, delayLevel, retryTimes int, timeout time.Duration)
func (api *MQClientAPI) TopicRouteInfoFromNameServer(topic string, timeout time.Duration) model.TopicRouteData

func (api *MQClientAPI) RegisterMessageFilterClass(consumerGroup, topic, className string, classCRC int, classBody []byte, timeout time.Duration) error
