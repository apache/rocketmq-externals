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

package kernel

import (
	"encoding/json"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/kernel/header"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"os"
	"strconv"
	"strings"
	"time"
)

//RocketMqClient this struct is for something common,for example
//1.brokerInfo
//2.routerInfo
//3.subscribeInfo
//4.heartbeat
type RocketMqClient interface {
	//get mqClient's clientId ip@pid
	getClientId() (clientId string)
	//get remoting client in mqClient
	getRemotingClient() (remotingClient *remoting.DefaultRemotingClient)
	//get topic subscribe info
	getTopicSubscribeInfo(topic string) (messageQueueList []*rocketmqm.MessageQueue)
	//getPublishTopicList
	getPublishTopicList() []string
	fetchMasterBrokerAddress(brokerName string) (masterAddress string)
	enqueuePullMessageRequest(pullRequest *model.PullRequest)
	dequeuePullMessageRequest() (pullRequest *model.PullRequest)
	findBrokerAddressInSubscribe(brokerName string, brokerId int, onlyThisBroker bool) (brokerAddr string, slave bool, found bool)
	tryToFindTopicPublishInfo(topic string) (topicPublicInfo *model.TopicPublishInfo, err error)
	findBrokerAddrByTopic(topic string) (addr string, ok bool)
	updateTopicRouteInfoFromNameServer(topic string) (err error)
	updateTopicRouteInfoFromNameServerUseDefaultTopic(topic string) (err error)
	sendHeartbeatToAllBroker(heartBeatData *model.HeartbeatData) (err error)
	clearExpireResponse()
	getMaxOffset(mq *rocketmqm.MessageQueue) int64
	searchOffset(mq *rocketmqm.MessageQueue, time time.Time) int64
}

//DEFAULT_TIMEOUT rocketmq client's default timeout
var DEFAULT_TIMEOUT int64 = 3000

//MqClientImpl RocketMqClient
type MqClientImpl struct {
	clientId                string
	remotingClient          *remoting.DefaultRemotingClient
	topicRouteTable         util.ConcurrentMap // map[string]*model.TopicRouteData   //topic | topicRoteData
	brokerAddrTable         util.ConcurrentMap //map[string]map[int]string          //brokerName | map[brokerId]address
	topicPublishInfoTable   util.ConcurrentMap //map[string]*model.TopicPublishInfo //topic | TopicPublishInfo //all use this
	topicSubscribeInfoTable util.ConcurrentMap //map[string][]*rocketmqm.MessageQueue   //topic | MessageQueue
	pullRequestQueue        chan *model.PullRequest
}

//mqClientInit create a mqClientInit instance
func mqClientInit(clientConfig *rocketmqm.MqClientConfig, clientRequestProcessor remoting.ClientRequestProcessor) (mqClientImpl *MqClientImpl) {
	mqClientImpl = &MqClientImpl{}
	mqClientImpl.clientId = buildMqClientImplId()
	mqClientImpl.topicRouteTable = util.NewConcurrentMap() // make(map[string]*model.TopicRouteData)
	mqClientImpl.brokerAddrTable = util.NewConcurrentMap() //make(map[string]map[int]string)
	mqClientImpl.remotingClient = remoting.RemotingClientInit(clientConfig, clientRequestProcessor)
	mqClientImpl.topicPublishInfoTable = util.NewConcurrentMap()   //make(map[string]*model.TopicPublishInfo)
	mqClientImpl.topicSubscribeInfoTable = util.NewConcurrentMap() //make(map[string][]*rocketmqm.MessageQueue)
	mqClientImpl.pullRequestQueue = make(chan *model.PullRequest, 1024)
	return
}

//getTopicSubscribeInfo
func (m *MqClientImpl) getTopicSubscribeInfo(topic string) (messageQueueList []*rocketmqm.MessageQueue) {
	value, ok := m.topicSubscribeInfoTable.Get(topic)
	if ok {
		messageQueueList = value.([]*rocketmqm.MessageQueue)
	}
	return
}
func (m *MqClientImpl) getMaxOffset(mq *rocketmqm.MessageQueue) int64 {
	brokerAddr := m.fetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		m.tryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = m.fetchMasterBrokerAddress(mq.BrokerName)
	}
	getMaxOffsetRequestHeader := &header.GetMaxOffsetRequestHeader{Topic: mq.Topic, QueueId: mq.QueueId}
	remotingCmd := remoting.NewRemotingCommand(remoting.GET_MAX_OFFSET, getMaxOffsetRequestHeader)
	response, err := m.remotingClient.InvokeSync(brokerAddr, remotingCmd, DEFAULT_TIMEOUT)
	if err != nil {
		return -1
	}
	queryOffsetResponseHeader := header.QueryOffsetResponseHeader{}
	queryOffsetResponseHeader.FromMap(response.ExtFields)
	return queryOffsetResponseHeader.Offset
}
func (m *MqClientImpl) searchOffset(mq *rocketmqm.MessageQueue, time time.Time) int64 {
	brokerAddr := m.fetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		m.tryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = m.fetchMasterBrokerAddress(mq.BrokerName)
	}
	timeStamp := util.CurrentTimeMillisInt64()
	searchOffsetRequestHeader := &header.SearchOffsetRequestHeader{Topic: mq.Topic, QueueId: mq.QueueId, Timestamp: timeStamp}
	remotingCmd := remoting.NewRemotingCommand(remoting.SEARCH_OFFSET_BY_TIMESTAMP, searchOffsetRequestHeader)
	response, err := m.remotingClient.InvokeSync(brokerAddr, remotingCmd, DEFAULT_TIMEOUT)
	if err != nil {
		return -1
	}
	queryOffsetResponseHeader := header.QueryOffsetResponseHeader{}

	queryOffsetResponseHeader.FromMap(response.ExtFields)
	return queryOffsetResponseHeader.Offset
}
func (m *MqClientImpl) getClientId() string {
	return m.clientId
}
func (m *MqClientImpl) getPublishTopicList() []string {
	var publishTopicList []string
	for _, topic := range m.topicPublishInfoTable.Keys() {
		publishTopicList = append(publishTopicList, topic)
	}
	return publishTopicList
}
func (m *MqClientImpl) getRemotingClient() *remoting.DefaultRemotingClient {
	return m.remotingClient
}

func (m *MqClientImpl) enqueuePullMessageRequest(pullRequest *model.PullRequest) {
	m.pullRequestQueue <- pullRequest
}
func (m *MqClientImpl) dequeuePullMessageRequest() (pullRequest *model.PullRequest) {
	pullRequest = <-m.pullRequestQueue
	return
}

func (m *MqClientImpl) clearExpireResponse() {
	m.remotingClient.ClearExpireResponse()
}

func (m *MqClientImpl) fetchMasterBrokerAddress(brokerName string) (masterAddress string) {
	value, ok := m.brokerAddrTable.Get(brokerName)
	if ok {
		masterAddress = value.(map[string]string)["0"]
	}
	return
}
func (m *MqClientImpl) tryToFindTopicPublishInfo(topic string) (topicPublicInfo *model.TopicPublishInfo, err error) {
	value, ok := m.topicPublishInfoTable.Get(topic)
	if ok {
		topicPublicInfo = value.(*model.TopicPublishInfo)
	}

	if topicPublicInfo == nil || !topicPublicInfo.JudgeTopicPublishInfoOk() {
		m.topicPublishInfoTable.Set(topic, &model.TopicPublishInfo{HaveTopicRouterInfo: false})
		err = m.updateTopicRouteInfoFromNameServer(topic)
		if err != nil {
			glog.Warning(err) // if updateRouteInfo error, maybe we can use the defaultTopic
		}
		value, ok := m.topicPublishInfoTable.Get(topic)
		if ok {
			topicPublicInfo = value.(*model.TopicPublishInfo)
		}
	}
	if topicPublicInfo.HaveTopicRouterInfo && topicPublicInfo.JudgeTopicPublishInfoOk() {
		return
	}
	//try to use the defaultTopic
	err = m.updateTopicRouteInfoFromNameServerUseDefaultTopic(topic)

	defaultValue, defaultValueOk := m.topicPublishInfoTable.Get(topic)
	if defaultValueOk {
		topicPublicInfo = defaultValue.(*model.TopicPublishInfo)
	}

	return
}

func (m MqClientImpl) getTopicRouteInfoFromNameServer(topic string, timeoutMillis int64) (*model.TopicRouteData, error) {
	requestHeader := &header.GetRouteInfoRequestHeader{
		Topic: topic,
	}
	var remotingCommand = remoting.NewRemotingCommand(remoting.GET_ROUTEINTO_BY_TOPIC, requestHeader)
	response, err := m.remotingClient.InvokeSync("", remotingCommand, timeoutMillis)

	if err != nil {
		return nil, err
	}
	if response.Code == remoting.SUCCESS {
		topicRouteData := new(model.TopicRouteData)
		bodyjson := strings.Replace(string(response.Body), ",0:", ",\"0\":", -1)
		bodyjson = strings.Replace(bodyjson, ",1:", ",\"1\":", -1) // fastJson key is string todo todo
		bodyjson = strings.Replace(bodyjson, "{0:", "{\"0\":", -1)
		bodyjson = strings.Replace(bodyjson, "{1:", "{\"1\":", -1)
		err = json.Unmarshal([]byte(bodyjson), topicRouteData)
		if err != nil {
			glog.Error(err, bodyjson)
			return nil, err
		}
		return topicRouteData, nil
	}
	return nil, fmt.Errorf("get topicRouteInfo from nameServer error[code:%d,topic:%s]", response.Code, topic)
}

func (m MqClientImpl) findBrokerAddressInSubscribe(brokerName string, brokerId int, onlyThisBroker bool) (brokerAddr string, slave bool, found bool) {
	slave = false
	found = false
	value, ok := m.brokerAddrTable.Get(brokerName)
	if !ok {
		return
	}
	brokerMap := value.(map[string]string)
	brokerAddr, ok = brokerMap[util.IntToString(brokerId)]
	slave = (brokerId != 0)
	found = ok

	if !found && !onlyThisBroker {
		var id string
		for id, brokerAddr = range brokerMap {
			slave = (id != "0")
			found = true
			break
		}
	}
	return
}

func (m MqClientImpl) updateTopicRouteInfoFromNameServer(topic string) (err error) {
	var (
		topicRouteData *model.TopicRouteData
	)
	//namesvr lock
	topicRouteData, err = m.getTopicRouteInfoFromNameServer(topic, DEFAULT_TIMEOUT)
	if err != nil {
		return
	}
	m.updateTopicRouteInfoLocal(topic, topicRouteData)
	return
}
func (m MqClientImpl) updateTopicRouteInfoFromNameServerUseDefaultTopic(topic string) (err error) {
	var (
		topicRouteData *model.TopicRouteData
	)
	//namesvr lock
	topicRouteData, err = m.getTopicRouteInfoFromNameServer(constant.DEFAULT_TOPIC, DEFAULT_TIMEOUT)
	if err != nil {
		return
	}

	for _, queueData := range topicRouteData.QueueDatas {
		defaultQueueData := constant.DEFAULT_TOPIC_QUEUE_NUMS
		if queueData.ReadQueueNums < defaultQueueData {
			defaultQueueData = queueData.ReadQueueNums
		}
		queueData.ReadQueueNums = defaultQueueData
		queueData.WriteQueueNums = defaultQueueData
	}
	m.updateTopicRouteInfoLocal(topic, topicRouteData)
	return
}
func (m MqClientImpl) updateTopicRouteInfoLocal(topic string, topicRouteData *model.TopicRouteData) (err error) {
	if topicRouteData == nil {
		return
	}
	// topicRouteData judgeTopicRouteData need update
	//needUpdate := true
	//if !needUpdate {
	//	return
	//}
	//update brokerAddrTable
	for _, brokerData := range topicRouteData.BrokerDatas {
		m.brokerAddrTable.Set(brokerData.BrokerName, brokerData.BrokerAddrs)
	}

	//update pubInfo for each
	topicPublishInfo := model.BuildTopicPublishInfoFromTopicRoteData(topic, topicRouteData)
	m.topicPublishInfoTable.Set(topic, topicPublishInfo)

	mqList := model.BuildTopicSubscribeInfoFromRoteData(topic, topicRouteData)
	m.topicSubscribeInfoTable.Set(topic, mqList)
	m.topicRouteTable.Set(topic, topicRouteData)
	return
}

func (m MqClientImpl) findBrokerAddrByTopic(topic string) (addr string, ok bool) {
	value, findValue := m.topicRouteTable.Get(topic)
	if !findValue {
		return "", false
	}
	topicRouteData := value.(*model.TopicRouteData)
	brokers := topicRouteData.BrokerDatas
	if brokers != nil && len(brokers) > 0 {
		brokerData := brokers[0]
		brokerData.BrokerAddrsLock.RLock()
		addr, ok = brokerData.BrokerAddrs["0"]
		brokerData.BrokerAddrsLock.RUnlock()

		if ok {
			return
		}
		for _, addr = range brokerData.BrokerAddrs {
			return addr, ok
		}
	}
	return
}

func buildMqClientImplId() (clientId string) {
	clientId = util.GetLocalIp4() + "@" + strconv.Itoa(os.Getpid())
	return
}

func (m MqClientImpl) sendHeartBeat(addr string, remotingCommand *remoting.RemotingCommand, timeoutMillis int64) error {
	remotingCommand, err := m.remotingClient.InvokeSync(addr, remotingCommand, timeoutMillis)
	if err != nil {
		glog.Error(err)
	} else {
		if remotingCommand == nil || remotingCommand.Code != remoting.SUCCESS {
			glog.Error("send heartbeat response  error")
		}
	}
	return err
}

func (m MqClientImpl) sendHeartbeatToAllBroker(heartBeatData *model.HeartbeatData) (err error) {
	for _, brokerTable := range m.brokerAddrTable.Items() {
		for brokerId, addr := range brokerTable.(map[string]string) {
			if len(addr) == 0 || brokerId != "0" {
				continue
			}
			data, err := json.Marshal(heartBeatData)
			if err != nil {
				glog.Error(err)
				return err
			}
			glog.V(2).Info("send heartbeat to broker look data[", string(data)+"]")
			remotingCommand := remoting.NewRemotingCommandWithBody(remoting.HEART_BEAT, nil, data)
			glog.V(2).Info("send heartbeat to broker[", addr+"]")
			m.sendHeartBeat(addr, remotingCommand, DEFAULT_TIMEOUT)

		}
	}
	return nil
}
