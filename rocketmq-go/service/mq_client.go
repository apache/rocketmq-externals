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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/consumer"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/producer"
	"sync"
	"github.com/golang/glog"
	"sync/atomic"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	"os"
	"strconv"
	"strings"
)

//this struct is for something common,for example
//1.brokerInfo
//2.routerInfo
//3.subscribeInfo
//4.heartbeat
type RocketMqClient interface {
	GetClientId() (clientId string)
	GetRemotingClient() (remotingClient *remoting.DefalutRemotingClient)
	GetTopicSubscribeInfo(topic string) (messageQueueList []*model.MessageQueue)
	GetPublishTopicList() []string
	FetchMasterBrokerAddress(brokerName string) (masterAddress string)
	EnqueuePullMessageRequest(pullRequest *model.PullRequest)
	DequeuePullMessageRequest() (pullRequest *model.PullRequest)
	FindBrokerAddressInSubscribe(brokerName string, brokerId int, onlyThisBroker bool) (brokerAddr string, slave bool, found bool)
	TryToFindTopicPublishInfo(topic string) (topicPublicInfo *model.TopicPublishInfo, err error)
	FindBrokerAddrByTopic(topic string) (addr string, ok bool)
	UpdateTopicRouteInfoFromNameServer(topic string) (err error)
	UpdateTopicRouteInfoFromNameServerUseDefaultTopic(topic string) (err error)
	SendHeartbeatToAllBroker(heartBeatData *model.HeartbeatData) (err error)
	ClearExpireResponse()
	GetMaxOffset(mq *model.MessageQueue) int64
	SearchOffset(mq *model.MessageQueue, time time.Time) int64
}

var DEFAULT_TIMEOUT int64 = 6000

// common
type MqClientImpl struct {
	ClientId                string
	remotingClient          remoting.RemotingClient
	TopicRouteTable         util.ConcurrentMap      // map[string]*model.TopicRouteData   //topic | topicRoteData
	BrokerAddrTable         util.ConcurrentMap      //map[string]map[int]string          //brokerName | map[brokerId]address
	TopicPublishInfoTable   util.ConcurrentMap      //map[string]*model.TopicPublishInfo //topic | TopicPublishInfo //all use this
	TopicSubscribeInfoTable util.ConcurrentMap      //map[string][]*model.MessageQueue   //topic | MessageQueue
	PullRequestQueue        chan *model.PullRequest //todo move
}

func MqClientInit(clientConfig *config.ClientConfig, clientRequestProcessor remoting.ClientRequestProcessor) (mqClientImpl *MqClientImpl) {
	mqClientImpl = &MqClientImpl{}
	mqClientImpl.ClientId = buildMqClientImplId()
	mqClientImpl.TopicRouteTable = util.New() // make(map[string]*model.TopicRouteData)
	mqClientImpl.BrokerAddrTable = util.New() //make(map[string]map[int]string)
	mqClientImpl.remotingClient = remoting.RemotingClientInit(clientConfig, clientRequestProcessor)
	mqClientImpl.TopicPublishInfoTable = util.New()   //make(map[string]*model.TopicPublishInfo)
	mqClientImpl.TopicSubscribeInfoTable = util.New() //make(map[string][]*model.MessageQueue)
	mqClientImpl.PullRequestQueue = make(chan *model.PullRequest, 1024)
	return
}
func (self *MqClientImpl) GetTopicSubscribeInfo(topic string) (messageQueueList []*model.MessageQueue) {
	value, ok := self.TopicSubscribeInfoTable.Get(topic)
	if ok {
		messageQueueList = value.([]*model.MessageQueue)
	}
	return
}
func (self *MqClientImpl) GetMaxOffset(mq *model.MessageQueue) int64 {
	brokerAddr := self.FetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		self.TryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = self.FetchMasterBrokerAddress(mq.BrokerName)
	}
	getMaxOffsetRequestHeader := &header.GetMaxOffsetRequestHeader{Topic: mq.Topic, QueueId: mq.QueueId}
	remotingCmd := remoting.NewRemotingCommand(remoting.GET_MAX_OFFSET, getMaxOffsetRequestHeader)
	response, err := self.remotingClient.InvokeSync(brokerAddr, remotingCmd, DEFAULT_TIMEOUT)
	if err != nil {
		return -1
	}
	queryOffsetResponseHeader := header.QueryOffsetResponseHeader{}
	queryOffsetResponseHeader.FromMap(response.ExtFields)
	glog.Info("op=look max offset result", string(response.Body))
	return queryOffsetResponseHeader.Offset
}
func (self *MqClientImpl) SearchOffset(mq *model.MessageQueue, time time.Time) int64 {
	brokerAddr := self.FetchMasterBrokerAddress(mq.BrokerName)
	if len(brokerAddr) == 0 {
		self.TryToFindTopicPublishInfo(mq.Topic)
		brokerAddr = self.FetchMasterBrokerAddress(mq.BrokerName)
	}
	timeStamp := time.UnixNano() / 1000000
	searchOffsetRequestHeader := &header.SearchOffsetRequestHeader{Topic: mq.Topic, QueueId: mq.QueueId, Timestamp: timeStamp}
	remotingCmd := remoting.NewRemotingCommand(remoting.SEARCH_OFFSET_BY_TIMESTAMP, searchOffsetRequestHeader)
	response, err := self.remotingClient.InvokeSync(brokerAddr, remotingCmd, DEFAULT_TIMEOUT)
	if err != nil {
		return -1
	}
	queryOffsetResponseHeader := header.QueryOffsetResponseHeader{}

	queryOffsetResponseHeader.FromMap(response.ExtFields)
	glog.Info("op=look search offset result", string(response.Body))
	return queryOffsetResponseHeader.Offset
}
func (self *MqClientImpl) GetClientId() string {
	return self.ClientId
}
func (self *MqClientImpl) GetPublishTopicList() []string {
	var publishTopicList []string
	for _, topic := range self.TopicPublishInfoTable.Keys() {
		publishTopicList = append(publishTopicList, topic)
	}
	return publishTopicList
}
func (self *MqClientImpl) GetRemotingClient() remoting.RemotingClient {
	return self.remotingClient
}

func (self *MqClientImpl) EnqueuePullMessageRequest(pullRequest *model.PullRequest) {
	self.PullRequestQueue <- pullRequest
}
func (self *MqClientImpl) DequeuePullMessageRequest() (pullRequest *model.PullRequest) {
	pullRequest = <-self.PullRequestQueue
	return
}

func (self *MqClientImpl) ClearExpireResponse() {
	//self.remotingClient.ClearExpireResponse()
}

func (self *MqClientImpl) FetchMasterBrokerAddress(brokerName string) (masterAddress string) {
	value, ok := self.BrokerAddrTable.Get(brokerName)
	if ok {
		masterAddress = value.(map[string]string)["0"]
	}
	return
}
func (self *MqClientImpl) TryToFindTopicPublishInfo(topic string) (topicPublicInfo *model.TopicPublishInfo, err error) {
	value, ok := self.TopicPublishInfoTable.Get(topic)
	if ok {
		topicPublicInfo = value.(*model.TopicPublishInfo)
	}

	if topicPublicInfo == nil || !topicPublicInfo.JudgeTopicPublishInfoOk() {
		self.TopicPublishInfoTable.Set(topic, &model.TopicPublishInfo{HaveTopicRouterInfo: false})
		err = self.UpdateTopicRouteInfoFromNameServer(topic)
		if err != nil {
			glog.Warning(err) // if updateRouteInfo error, maybe we can use the defaultTopic
		}
		value, ok := self.TopicPublishInfoTable.Get(topic)
		if ok {
			topicPublicInfo = value.(*model.TopicPublishInfo)
		}
	}
	if topicPublicInfo.HaveTopicRouterInfo && topicPublicInfo.JudgeTopicPublishInfoOk() {
		return
	}
	//try to use the defaultTopic
	err = self.UpdateTopicRouteInfoFromNameServerUseDefaultTopic(topic)

	defaultValue, defaultValueOk := self.TopicPublishInfoTable.Get(topic)
	if defaultValueOk {
		topicPublicInfo = defaultValue.(*model.TopicPublishInfo)
	}

	return
}

func (self MqClientImpl) GetTopicRouteInfoFromNameServer(topic string, timeoutMillis int64) (*model.TopicRouteData, error) {
	requestHeader := &header.GetRouteInfoRequestHeader{
		Topic: topic,
	}
	var remotingCommand = remoting.NewRemotingCommand(remoting.GET_ROUTEINTO_BY_TOPIC, requestHeader)
	response, err := self.remotingClient.InvokeSync("", remotingCommand, timeoutMillis)

	if err != nil {
		return nil, err
	}
	if response.Code == remoting.SUCCESS {
		//todo  it's dirty
		topicRouteData := new(model.TopicRouteData)
		bodyjson := strings.Replace(string(response.Body), ",0:", ",\"0\":", -1)
		bodyjson = strings.Replace(bodyjson, ",1:", ",\"1\":", -1) // fastJson的key没有引号 需要通用的方法
		bodyjson = strings.Replace(bodyjson, "{0:", "{\"0\":", -1)
		bodyjson = strings.Replace(bodyjson, "{1:", "{\"1\":", -1)
		err = json.Unmarshal([]byte(bodyjson), topicRouteData)
		if err != nil {
			glog.Error(err, bodyjson)
			return nil, err
		}
		return topicRouteData, nil
	} else {
		return nil, errors.New(fmt.Sprintf("get topicRouteInfo from nameServer error[code:%d,topic:%s]", response.Code, topic))
	}
}

func (self MqClientImpl) FindBrokerAddressInSubscribe(brokerName string, brokerId int, onlyThisBroker bool) (brokerAddr string, slave bool, found bool) {
	slave = false
	found = false
	value, ok := self.BrokerAddrTable.Get(brokerName)
	if !ok {
		return
	}
	brokerMap := value.(map[string]string)
	//self.brokerAddrTableLock.RUnlock()
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

func (self MqClientImpl) UpdateTopicRouteInfoFromNameServer(topic string) (err error) {
	var (
		topicRouteData *model.TopicRouteData
	)
	//namesvr lock
	//topicRouteData = this.MqClientImplAPIImpl.getTopicRouteInfoFromNameServer(topic, 1000 * 3);
	topicRouteData, err = self.GetTopicRouteInfoFromNameServer(topic, 1000*3)
	if err != nil {
		return
	}
	self.updateTopicRouteInfoLocal(topic, topicRouteData)
	return
}
func (self MqClientImpl) UpdateTopicRouteInfoFromNameServerUseDefaultTopic(topic string) (err error) {
	var (
		topicRouteData *model.TopicRouteData
	)
	//namesvr lock
	//topicRouteData = this.MqClientImplAPIImpl.getTopicRouteInfoFromNameServer(topic, 1000 * 3);
	topicRouteData, err = self.GetTopicRouteInfoFromNameServer(constant.DEFAULT_TOPIC, 1000*3)
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
	self.updateTopicRouteInfoLocal(topic, topicRouteData)
	return
}
func (self MqClientImpl) updateTopicRouteInfoLocal(topic string, topicRouteData *model.TopicRouteData) (err error) {
	if topicRouteData == nil {
		return
	}
	// topicRouteData judgeTopicRouteData need update
	needUpdate := true
	if !needUpdate {
		return
	}
	//update brokerAddrTable
	for _, brokerData := range topicRouteData.BrokerDatas {
		self.BrokerAddrTable.Set(brokerData.BrokerName, brokerData.BrokerAddrs)
	}

	//update pubInfo for each
	topicPublishInfo := model.BuildTopicPublishInfoFromTopicRoteData(topic, topicRouteData)
	self.TopicPublishInfoTable.Set(topic, topicPublishInfo)

	mqList := model.BuildTopicSubscribeInfoFromRoteData(topic, topicRouteData)
	self.TopicSubscribeInfoTable.Set(topic, mqList)
	self.TopicRouteTable.Set(topic, topicRouteData)
	return
}

func (self MqClientImpl) FindBrokerAddrByTopic(topic string) (addr string, ok bool) {
	value, findValue := self.TopicRouteTable.Get(topic)
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

func (self MqClientImpl) sendHeartBeat(addr string, remotingCommand *remoting.RemotingCommand, timeoutMillis int64) error {
	remotingCommand, err := self.remotingClient.InvokeSync(addr, remotingCommand, timeoutMillis)
	if err != nil {
		glog.Error(err)
	} else {
		if remotingCommand == nil || remotingCommand.Code != remoting.SUCCESS {
			glog.Error("send heartbeat response  error")
		}
	}
	return err
}

func (self MqClientImpl) SendHeartbeatToAllBroker(heartBeatData *model.HeartbeatData) (err error) {
	//self.brokerAddrTableLock.RLock()

	for _, brokerTable := range self.BrokerAddrTable.Items() {
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
			self.sendHeartBeat(addr, remotingCommand, 3000)

		}
	}
	return nil
}

const lockTimeout = 3 * time.Second

type ExecutorService interface {
	Executor(func(), delay, period time.Duration)
}

type MQClient struct {
	cfg config.ClientConfig
	index int
	clientID string
	bootTimestamp time.Time
	producerTable map[string]producer.MQProducer
	consumerTable map[string]consumer.MQConsumer
	api *MQClientAPI
	topicRouteTable map[string]model.TopicRouteData
	nameSrvMu sync.RWMutex
	heartbeatMu sync.RWMutex
	brokerAddressTable map[string]map[int64]string
	scheduledExecutorService  ExecutorService
	storeTimesTotal int64
}

func NewMQClient(cfg config.ClientConfig, index int, clientID string, hook remoting.RPCHook) MQClient {
	return MQClient{}
}

func (c *MQClient) Start() error

func (c *MQClient) startScheduleTask() {
	if c.cfg.NameServerAddress() == "" {
		c.scheduledExecutorService.Executor(func() {
			c.api.FetchNameServerAddress()
		}, 10 * time.Second, 60 * 2 * time.Second)
	}

	c.scheduledExecutorService.Executor(func() {
		topicList := util.NewSet()

		// consumer
		for _, v := range c.consumerTable {
			if v != nil {
				if subList := v.Subscriptions(); subList != nil {
					topicList.Add(subList.Flatten())
				}
			}
		}

		// TODO producer

		for _, data := range topicList.Flatten() {
			c.UpdateTopicRouteInfoFromNameServer(data.(model.SubscriptionData).Topic, false)
		}
	}, 10 * time.Millisecond, c.cfg.PullNameServerInteval())

	c.scheduledExecutorService.Executor(func() {
		c.CleanOfflineBroker()
		c.SendHeartbeatToAllBrokerWithLock()
	}, time.Second, c.cfg.HeartbeatBrokerInterval())

	c.scheduledExecutorService.Executor(func() {
		c.persistAllConsumerOffset()
	}, 10 * time.Second, c.cfg.PersistConsumerOffsetInterval())

	c.scheduledExecutorService.Executor(func() {
		c.adjustThreadPool()
	}, time.Minute, time.Minute)
}

func (c *MQClient) ClientID() string {
	return c.clientID
}

func (c *MQClient) UpdateTopicRouteInfoFromNameServer(topic string, isDefault bool) error {
	// TODO default?
	c.nameSrvMu.Lock()
	defer c.nameSrvMu.Unlock()

	topicRouteData, err := c.api.TopicRouteInfoFromNameServer(topic, 3 * time.Second)

	if err != nil {
		return err
	}

	old := c.topicRouteTable[topic]

	changed := topicRouteDataIsChange(old, topicRouteData)
	if !changed {
		changed = c.isNeedUpdateTopicRouteInfo(topic)
	} else {
		glog.Infof("The topic[%s] route info changed, old:[%s], new:[%s]", topic, old, topicRouteData)
	}

	if changed {
		cloneData := topicRouteData.CloneTopicRouteData()
		for _, bd := range topicRouteData.BrokerDatas() {
			c.brokerAddressTable[bd.BrokerName] = bd.BrokerAddress
		}

		publishInfo := TopicRouteData2TopicPublishInfo(topic, topicRouteData)
		publishInfo.SetHaveTopicRouteInfo(true)

		// TODO update producer topic publish info

		subscribeInfo := TopicRouteData2TopicSubscribeInfo(topic, topicRouteData)

		for _, cs := range c.consumerTable {
			cs.UpdateTopicSubscribeInfo(topic, subscribeInfo)
		}

		glog.Infof("update topicRouteTable: TopicRouteData[%s]", cloneData)
	}
	return nil
}

func (c *MQClient) CleanOfflineBroker() {
	c.nameSrvMu.Lock()
	defer c.nameSrvMu.Unlock()

	updatedTable := make(map[string]map[int64]string)

	for brokerName, table := range c.brokerAddressTable {
		cloneAddressTable := make(map[int64]string)
		copy(cloneAddressTable, table)

		for id, address := range cloneAddressTable {
			if !c.isBrokerAddressExistInTopicRouteTable(address) {
				delete(cloneAddressTable, id)
				glog.Infof("the broker:[%s], address:[%s] is offline, remove it.", brokerName, address)
			}
		}

		if len(cloneAddressTable) == 0 {
			delete(c.brokerAddressTable, brokerName)
			glog.Infof("the broker:[%s] name's host is offline, remote it.")
		} else {
			updatedTable[brokerName] = cloneAddressTable
		}
	}

	for k, v := range updatedTable {
		c.brokerAddressTable[k] = v
	}
}

func (c *MQClient) SendHeartbeatToAllBrokerWithLock() {
	c.heartbeatMu.Lock()
	defer c.heartbeatMu.Unlock()

	c.sendHeartbeatToAllBroker()
	c.uploadFilterClassSource()
}

func (c *MQClient) persistAllConsumerOffset() {
	for _, v := range c.consumerTable {
		v.PersistConsumerOffset()
	}
}

// TODO
func (c *MQClient) adjustThreadPool()

func (c *MQClient) isBrokerAddressExistInTopicRouteTable(topic string) bool {
	for _, data := range c.topicRouteTable { // TODO optimize
		for _, bd := range data.BrokerDatas() {
			for _, address := range bd.BrokerAddress {
				if address == topic {
					return true
				}
			}
		}
	}
	return false
}

func (c *MQClient) sendHeartbeatToAllBroker() {
	producerEmpty := len(c.producerTable) == 0
	consumerEmpty := len(c.consumerTable) == 0

	if producerEmpty && consumerEmpty {
		glog.Warning("sending heartbeat, but consumer and produer nums is 0.")
	}

	hbd := c.prepareHeartbeatData()
	times := atomic.AddInt64(&c.storeTimesTotal, 1)

	for brokerName, table := range c.brokerAddressTable {
		for id, address := range table {
			if address != "" && id != -1 { // TODO MixAll{
				continue
			}

			c.api.SendHeartBeat(address, hbd, 3 * time.Second)

			if times % 20 == 0 {
				glog.Infof("sned heart beat to broker:[%s, %v, %s]", brokerName, id, address)
				glog.Info(hbd)
			}
		}
	}
}

// TODO
func (c *MQClient) uploadFilterClassSource(){}


type ConsumerData struct {
	groupName string
	cType ConsumeType
	messageModel MessageModel
	where ConsumeFromWhere
	subDatas *util.Set//map[*model.SubscriptionData]bool
	unitMode bool
}

func (c *MQClient) prepareHeartbeatData() HeartbeatData {
	data := HeartbeatData{clientID: c.clientID}

	for _, cs := range c.consumerTable {
		consumerData := ConsumerData{
			groupName: cs.GroupName(),
			cType: cs.ConsumeType(),
			messageModel: cs.MessageModel(),
			where: cs.ConsumeFromWhere(),
			unitMode: cs.UnitMode(),
			subDatas: util.NewSet(cs.Subscriptions().Flatten()),
		}

		data.consumerDataSet.Add(consumerData)
	}

	// TODO producer

	return data
}

func (c *MQClient) isBrokerInNameServer(brokerAddress string)
func (c *MQClient) uploadFilterClassToAllFilterServer( /*TODO*/ )
func (c *MQClient) isNeedUpdateTopicRouteInfo(topic string) bool
func (c *MQClient) Shutdown()
func (c *MQClient) RegisterConsumer(group string, consumer rocketmq.MQConsumer) bool
func (c *MQClient) RemoveConsumer(group string)
func (c *MQClient) RemoveClient(producerGroup, consumerGroup string)
func (c *MQClient) RegisterProducer(group string, producer rocketmq.RocketMQProducer) bool
func (c *MQClient) RemoveProducer(group string)
func (c *MQClient) RebalanceImmediately()
func (c *MQClient) DoRebalance()
func (c *MQClient) SelectProducer(group string) rocketmq.RocketMQProducer
func (c *MQClient) SelectConsumer(group string) rocketmq.MQConsumer
func (c *MQClient) FindBrokerAddressInPublish(brokerName string) string
type FindBrokerResult struct {
	BrokerAddress string
	Slave         bool
}

func (c *MQClient) FindBrokerAddressInSubscribe(brokerName string, brokerID int64, onlyThisBorker bool) *FindBrokerResult {
	return nil
}

func (c *MQClient) FindConsumerIDList(topic, group string) []string
func (c *MQClient) FindBrokerAddressByTopic(topic string)
func (c *MQClient) ResetOffset(topic, group string, offsetTable map[message.MessageQueue]int64)
func (c *MQClient) ConsumerStatus(topic, group string) map[message.MessageQueue]int64
func (c *MQClient) GetAnExistTopicRouteData(topic string) model.TopicRouteData
func (c *MQClient) ScheduledExecutorService() // TODO return value
func (c *MQClient) PullMessageService()       // TODO return value
func (c *MQClient) DefaultMQProducer()        // TODO return value
func (c *MQClient) TopicRouteTable() map[string]model.TopicRouteData
func (c *MQClient) ConsumeMessageDirectly(msg message.MessageExt, consumerGroup, brokerName string) // TODO return value
func (c *MQClient) ConsumerRunningInfo(consumerGroup string)                                        // TODO return value
func (c *MQClient) ConsumerStatusManager()                                                          // TODO return value
// exist same name method, but return []MessageQueue
func TopicRouteData2TopicPublishInfo(topic string, route model.TopicRouteData) model.TopicPublishInfo

func TopicRouteData2TopicSubscribeInfo(topic string, route model.TopicRouteData) map[*message.MessageQueue]bool
func topicRouteDataIsChange(oldData, newData model.TopicRouteData) bool
