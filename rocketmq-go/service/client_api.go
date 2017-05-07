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
	"bytes"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/golang/glog"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
)

func init() {
	os.Setenv(remoting.RemotingVersionKey, strconv.Itoa(rocketmq.CurrentVersion))
}

var sendSmartMsg bool = false // TODO _ := strconv.ParseBool(os.Getenv("org.apache.rocketmq.client.sendSmartMsg"))

type TopAddressing struct {
	nsAddress string
	wsAddress string
	unitName  string
}

func clearNewLine(str string) string {
	newStr := strings.Trim(str, " ")
	index := strings.Index(newStr, "\r")
	if index != -1 {
		return newStr[:index]
	}

	index = strings.Index(newStr, "\n")
	if index != -1 {
		return newStr[:index]
	}
	return newStr
}

func (ta *TopAddressing) fetchNSAddress(verbose bool) string {
	if ta.wsAddress == "" {
		glog.Fatalf("TopAddressing wsAddress is nil!")
	}
	var url string = ta.wsAddress
	if ta.unitName != "" {
		url = fmt.Sprintf("%s-%s?nofix=1", url, ta.unitName)
	}
	response, err := http.Get(url)
	if err != nil {
		glog.Fatalf("fetch name server address Error: %s", err.Error())
	}
	if response.StatusCode == 200 {
		body := string(response.Body)
		if body == "" {
			glog.Error("fetch nameserver address is nil!")
		} else {
			return clearNewLine(body)
		}
	} else {
		glog.Errorf("fetch nameserver address failed. statusCode=%v", response.StatusCode)
	}

	glog.Errorf("connect to %s failed, maybe the domain not bind in /etc.hosts", url)
	return ""
}

type MQClientAPI struct {
	crp               *remoting.ClientRemotingProcessor
	config            *config.ClientConfig
	rClient           *remoting.RemotingClient
	topAddressing     *TopAddressing
	nameServerAddress string
}

// TODO unfinished
func NewMQClientAPI(cfg *config.ClientConfig, processor *remoting.ClientRemotingProcessor, hook remoting.RPCHook) *MQClientAPI {
	api := &MQClientAPI{
		crp:           processor,
		config:        cfg,
		rClient:       &remoting.RemotingClient{}, //TODO
		topAddressing: &TopAddressing{},           // TODO TopAddressing(MixAll.WS_ADDR, clientConfig.getUnitName());
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
	ads := api.topAddressing.fetchNSAddress(true)
	if ads == "" {
		glog.Fatal("Fetch NameServer Address Error.")
	}
	if ads != api.nameServerAddress {
		glog.Infof("name server address changed, old=%s, new=%s", api.nameServerAddress, ads)
		api.UpdateNameServerAddressList(ads)
		api.nameServerAddress = ads
	}
	return api.nameServerAddress
}

func (api *MQClientAPI) UpdateNameServerAddressList(ads string) {
	addressList := strings.Split(ads, ";")
	api.rClient.UpdateNameServerAddressList(addressList)
}

func (api *MQClientAPI) Start() {
	api.rClient.Start()
}
func (api *MQClientAPI) Shutdown() {
	api.rClient.Shutdown()
}

// TODO optimize
func (api *MQClientAPI) CreateTopic(key, newTopic string, queueNum, topicSysFlag int, timeout time.Duration) error {
	topicRouteData, err := api.TopicRouteInfoFromNameServer(key, timeout)

	if err != nil {
		return err
	}

	brokerDatas := topicRouteData.BrokerDatas()
	if len(brokerDatas) == 0 {
		glog.Fatal("Not found broker, maybe key is wrong")
	}

	var strBuffer bytes.Buffer

	for _, data := range brokerDatas {
		address := data.BrokerAddress[-1] // TODO MixAll.MASTER_ID
		if address != "" {
			var createOK bool = false
			for i := 0; i < 5; i++ {
				cfg := config.NewTopicConfig(newTopic, queueNum, queueNum, topicSysFlag)
				err := api.crTopic(address, key, cfg, timeout)

				if err != nil {
					if i == 4 {
						return model.NewMQClientError(0,
							fmt.Sprintf("create topic to broker ERROR: %s", err.Error()))
					}
					continue
				}
				createOK = true
				break
			}

			if createOK {
				strBuffer.WriteString(data.BrokerName)
				strBuffer.WriteString(":")
				strBuffer.WriteString(fmt.Sprint(queueNum))
				strBuffer.WriteString(";")
			}
		}
	}

	return nil
}

func (api *MQClientAPI) crTopic(address, defaultTopic string, cfg config.TopicConfig, timeout time.Duration) error {
	requestHeader := header.CreateTopicRequestHeader{ // TODO optimize with TopicConfig directly
		Topic:           cfg.TopicName,
		DefaultTopic:    defaultTopic,
		ReadQueueNum:    cfg.ReadQueueNum,
		WriteQueueNum:   cfg.WriteQueueNum,
		Perm:            cfg.Perm,
		TopicFilterType: cfg.TopicFilter.String(),
		TopicSysFlag:    cfg.TopicSysFlag,
		Order:           cfg.Order,
	}

	request := remoting.CreateRemotingCommand(model.UpdateAndCreateTopic, requestHeader)
	response, err := api.rClient.InvokeSync(address, request, timeout)
	if err != nil {
		glog.Errorf("Create Topic %s ERROR: %s!", address, err.Error())
		return err
	}

	if response.Code != model.Success {
		return model.NewMQClientError(response.Code, fmt.Sprintf("Create Topic Failed, Response is: %v, Remark is %v",
			response.Code, response.Remark))
	}
	return nil
}

func buildRequest(requestHeader *header.SendMessageRequestHeader) *remoting.RemotingCommand {
	var request *remoting.RemotingCommand
	if sendSmartMsg {
		// TODO Send With V2
	} else {
		request = remoting.CreateRemotingCommand(model.SendMsg, requestHeader)
	}
	return request
}

// TODO refactor API
func (api *MQClientAPI) SendMessageOneWay(address string,
	msg message.Message,
	requestHeader header.SendMessageRequestHeader,
	timeout time.Duration,
) (*model.SendResult, error) {
	request := buildRequest(&requestHeader)
	request.SetBody(msg.Body)
	return nil, api.rClient.InvokeOneWay(address, request, timeout)
}

func (api *MQClientAPI) SendMessageSync(address, brokerName string,
	msg message.Message,
	timeout time.Duration,
	requestHeader header.SendMessageRequestHeader) (*model.SendResult, error) {
	request := buildRequest(&requestHeader)
	response, err := api.rClient.InvokeSync(address, request, timeout)
	if err != nil {
		// TODO
	}

	return api.processSendResponse(brokerName, msg, response)
}

func (api *MQClientAPI) sendMessageAsync(address, brokerName string,
	msg message.Message, timeout time.Duration, requestHeader header.SendMessageRequestHeader,
	callback model.SendCallback, info model.TopicPublishInfo /*TODO*/)

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
			regionID = "DefaultRegion" // TODO MixAll.DEFAULT_TRACE_REGION_ID
		}
		sendResult.SetRegionID(regionID)

		traceOn, found := response.ExtFields[message.MessageConst.PropertyTraceSwitch]
		if traceOn == "false" {
			sendResult.SetTraceOn(false)
		} else {
			sendResult.SetTraceOn(true)
		}

		return sendResult, nil
	}

	return nil, model.NewMQBrokerError(response.Code, response.Remark)
}

func (api *MQClientAPI) PullMessageSync(address string, requestHeader header.PullMessageRequestHeader,
	timeout time.Duration) (model.PullResult, error) {
	request := remoting.CreateRemotingCommand(model.PullMsg, &requestHeader)

	response, err := api.rClient.InvokeSync(address, request, timeout)
	if err != nil {
		glog.Fatalf("InvokeSync Error: %s", err.Error())
	}
	return api.processPullResponse(response)
}

func (api *MQClientAPI) PullMessageAsync(address string,
	requestHeader header.PullMessageRequestHeader,
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
	pr := model.NewPullResult(pullStatus, rh.NextBeginOffset, rh.MinOffset, rh.MaxOffset, nil)
	pr.SetSuggestWhichBrokerID(rh.SuggestWhichBrokerID)
	pr.SetMessageBinary(response.Body)
	return pr, err
}

type HeartbeatData struct {
	clientID        string
	producerDataSet map[string]bool // producers
	consumerDataSet *util.Set//map[ConsumerData]bool // consumers
}

func (hb HeartbeatData) encode() []byte {
	// TODO
	return nil
}

func (api *MQClientAPI) SendHeartBeat(address string, hbd HeartbeatData, timeout time.Duration) {
	request := remoting.CreateRemotingCommand(model.HeartBeat, nil)
	request.SetBody(hbd.encode())

	response, err := api.rClient.InvokeSync(address, request, timeout)

	if err != nil {
		glog.Errorf("Send Heart Beat ERROR: %s", err.Error())
	}

	if response.Code != model.Success {
		glog.Fatalf("Synchronize Heart Beat FAILED! Status: %v", response.Code)
	}
}

func (api *MQClientAPI) ConsumerSendMessageBack(address, consumerGroup string, msgX message.MessageExt,
	delayLevel, retryTimes int, timeout time.Duration) {
	requestHeader := header.ConsumerSendMsgBackRequestHeader{
		Offset:            msgX.CommitLogOffset,
		ConsumerGroup:     consumerGroup,
		DelayLevel:        delayLevel,
		OriginMsgID:       msgX.MsgId,
		OriginTopic:       msgX.Topic,
		UnitMode:          false,
		MaxReconsumeTimes: retryTimes,
	}

	request := remoting.CreateRemotingCommand(model.ConsumerSendMsgBack, requestHeader)
	response, err := api.rClient.InvokeSync(address /* TODO MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled()*/, request, timeout)

	if err != nil { // TODO optimize
		glog.Errorf("Consumer Send Message Back ERROR: %s", err.Error())
	}

	if response.Code != model.Success {
		glog.Fatalf("Consumer Send Message Back FAILED! Status: %v", response.Code)
	}
}

func (api *MQClientAPI) TopicRouteInfoFromNameServer(topic string, timeout time.Duration) (model.TopicRouteData, error) {
	requestHeader := header.RouteInfoRequestHeader{Topic: topic}
	request := remoting.CreateRemotingCommand(model.GetRouteinfoByTopic, requestHeader)

	response, err := api.rClient.InvokeSync("", request, timeout)

	if err != nil { // TODO optimize
		glog.Errorf("GET TopicRouteInfo From NameServer ERROR: %s", err.Error())
		return nil, model.NewMQBrokerError(0, fmt.Sprintf("GET TopicRouteInfo From NameServer ERROR: %s", err.Error()))
	}

	switch response.Code {
	case model.Success:
		body := response.Body
		if body != nil {
			return nil, nil // TODO return TopicRouteData.decode(body, TopicRouteData.class);
		}
	case model.TopicNotExist:
		glog.Error("GET TopicRouteInfo From NameServer Failed, Because of Topic Not Exist.")
		return nil, model.NewMQBrokerError(0, "GET TopicRouteInfo From NameServer Failed, Because of Topic Not Exist.")
	}

	return nil, nil
}

func (api *MQClientAPI) LockBatchMQ(address string, body *model.LockBatchRequestBody,
	timeout time.Duration) (*util.Set, error) {
	request := remoting.CreateRemotingCommand(model.LockBatchMq, nil)
	request.SetBody(body.Encode())

	response, err := api.rClient.InvokeSync(address, request, timeout) // MIXALL
	if err != nil  {
		return nil, err
	}

	if response.Code != model.Success {
		return nil, model.NewMQBrokerError(response.Code, response.Remark)
	}
	responseBody := model.LockBatchRequestBodyDecode(response.Body)
	return responseBody.MqSet, nil
}

func (api *MQClientAPI) UnlockBatchMQ(address string, body *model.UnlockBatchRequestBody,
	timeout time.Duration, oneWay bool) error {
	request := remoting.CreateRemotingCommand(model.UNLockBatchMq, nil)

	request.SetBody(body.Encode())

	if oneWay {
		return api.rClient.InvokeOneWay(address, request, timeout)
	} else {
		response, err := api.rClient.InvokeSync(address, request, timeout) // MIXALL
		 if err != nil  {
			 return err
		 }
		if response.Code != model.Success {
			return model.NewMQBrokerError(response.Code, response.Remark)
		}
	}
	return nil
}

// TODO
func (api *MQClientAPI) RegisterMessageFilterClass(consumerGroup, topic, className string, classCRC int, classBody []byte, timeout time.Duration) error
