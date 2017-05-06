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
)

type RocketMqClient interface {
}

type MQClient struct {
}

func NewMQClient(cfg config.ClientConfig, index int, clientID string, hook remoting.RPCHook) MQClient {
	return MQClient{}
}

func (c *MQClient) Start() error

func (c *MQClient) startScheduleTask()
func (c *MQClient) ClientID() string
func (c *MQClient) UpdateTopicRouteInfoFromNameServer(topic string, isDefault bool) // TODO consider with parameters
func (c *MQClient) CleanOfflineBroker()
func (c *MQClient) SendHeartbeatToAllBrokerWithLock()
func (c *MQClient) persistAllConsumerOffset()
func (c *MQClient) adjustThreadPool()
func (c *MQClient) isBrokerAddressExistInTopicRouteTable(topic string)
func (c *MQClient) sendHeartbeatToAllBroker()
func (c *MQClient) uploadFilterClassSource()
func (c *MQClient) prepareHeartbeatData()
func (c *MQClient) isBrokerInNameServer(brokerAddress string)
func (c *MQClient) uploadFilterClassToAllFilterServer( /*TODO*/ )
func (c *MQClient) topicRouteDataIsChange(oldData, newData model.TopicRouteData) bool
func (c *MQClient) isNeedUpdateTopicRouteInfo(topic string)
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
