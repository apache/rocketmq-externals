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

package model

import (
	//"fmt"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"sync"
)

//
//type BrokerData struct {
//}
//
//type TopicRouteData struct {
//	orderTopicConf    string
//	queueDatas        []*message.MessageQueue
//	brokerDatas       []*BrokerData
//	filterServerTable map[string][]string
//}
//
//func NewTopicRouteData() *TopicRouteData {
//	return &TopicRouteData{}
//}
//
//func (route *TopicRouteData) CloneTopicRouteData() (clonedRouteData *TopicRouteData) {
//	clonedRouteData = &TopicRouteData{
//		route.orderTopicConf,
//		route.queueDatas,
//		route.brokerDatas,
//		route.filterServerTable,
//	}
//	// TODO: to complete
//	return
//}
//
//func (route *TopicRouteData) QueueDatas() []*message.MessageQueue {
//	return route.queueDatas
//}
//
//func (route *TopicRouteData) SetQueueDatas(data []*message.MessageQueue) {
//	route.queueDatas = data
//}
//
//func (route *TopicRouteData) BrokerDatas() []*BrokerData {
//	return route.brokerDatas
//}
//
//func (route *TopicRouteData) SetBrokerDatas(data []*BrokerData) {
//	route.brokerDatas = data
//}
//
//func (route *TopicRouteData) FilterServerTable() map[string][]string {
//	return route.filterServerTable
//}
//
//func (route *TopicRouteData) SetFilterServerTable(data map[string][]string) {
//	route.filterServerTable = data
//}
//
//func (route *TopicRouteData) OrderTopicConf() string {
//	return route.orderTopicConf
//}
//
//func (route *TopicRouteData) SetOrderTopicConf(s string) {
//	route.orderTopicConf = s
//}
//
//func (route *TopicRouteData) HashCode() (result int) {
//	prime := 31
//	result = 1
//	result *= prime
//	// TODO
//
//	return
//}
//
//func (route *TopicRouteData) Equals(route1 interface{}) bool {
//	if route == nil {
//		return true
//	}
//	if route1 == nil {
//		return false
//	}
//	//value, ok := route1.(TopicRouteData)
//	//if !ok {
//	//	return false
//	//}
//	// TODO
//	//if route.brokerDatas == nil && value.brokerDatas != nil || len(route.brokerDatas) != len(value.brokerDatas) {
//	//	return false
//	//}
//	//
//	//if route.orderTopicConf == "" && value.orderTopicConf != "" || route.orderTopicConf != value.orderTopicConf {
//	//	return false
//	//}
//	//
//	//if route.queueDatas == nil && value.queueDatas != nil || route.queueDatas != value.queueDatas {
//	//	return false
//	//}
//	//
//	//if route.filterServerTable == nil && value.filterServerTable != nil ||
//	//	route.filterServerTable != value.filterServerTable {
//	//	return false
//	//}
//	return true
//}
//
//func (route *TopicRouteData) String() string {
//	return fmt.Sprintf("TopicRouteData [orderTopicConf=%s, queueDatas=%s, brokerDatas=%s, filterServerTable=%s]",
//		route.orderTopicConf, route.queueDatas, route.brokerDatas, route.filterServerTable)
//}

type TopicRouteData struct {
	OrderTopicConf string
	QueueDatas     []*QueueData
	BrokerDatas    []*BrokerData
}
type QueueData struct {
	BrokerName     string
	ReadQueueNums  int32
	WriteQueueNums int32
	Perm           int32
	TopicSynFlag   int32
}
type BrokerData struct {
	BrokerName      string
	BrokerAddrs     map[string]string
	BrokerAddrsLock sync.RWMutex
}
