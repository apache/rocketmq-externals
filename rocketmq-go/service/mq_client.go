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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/consumer"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/producer"
	"sync"
	"github.com/golang/glog"
	"sync/atomic"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
)

type RocketMqClient interface {
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
