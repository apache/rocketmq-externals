package service

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
)

type BrokerData struct {
}

type TopicRouteData struct {
	orderTopicConf    string
	queueDatas        []*message.MessageQueue
	brokerDatas       []*BrokerData
	filterServerTable map[string][]string
}

func NewTopicRouteData() *TopicRouteData {
	return &TopicRouteData{}
}

func (route *TopicRouteData) CloneTopicRouteData() (clonedRouteData *TopicRouteData) {
	clonedRouteData = &TopicRouteData{
		route.orderTopicConf,
		route.queueDatas,
		route.brokerDatas,
		route.filterServerTable,
	}
	// TODO: to complete
	//if (this.queueDatas != null) {
	//topicRouteData.getQueueDatas().addAll(this.queueDatas);
	//}
	//
	//if (this.brokerDatas != null) {
	//topicRouteData.getBrokerDatas().addAll(this.brokerDatas);
	//}
	//
	//if (this.filterServerTable != null) {
	//topicRouteData.getFilterServerTable().putAll(this.filterServerTable);
	//}
	return
}

func (route *TopicRouteData) QueueDatas() []*message.MessageQueue {
	return route.queueDatas
}

func (route *TopicRouteData) SetQueueDatas(data []*message.MessageQueue) {
	route.queueDatas = data
}

func (route *TopicRouteData) BrokerDatas() []*BrokerData {
	return route.brokerDatas
}

func (route *TopicRouteData) SetBrokerDatas(data []*BrokerData) {
	route.brokerDatas = data
}

func (route *TopicRouteData) FilterServerTable() map[string][]string {
	return route.filterServerTable
}

func (route *TopicRouteData) SetFilterServerTable(data map[string][]string) {
	route.filterServerTable = data
}

func (route *TopicRouteData) OrderTopicConf() string {
	return route.orderTopicConf
}

func (route *TopicRouteData) SetOrderTopicConf(s string) {
	route.orderTopicConf = s
}

func (route *TopicRouteData) HashCode() (result int) {
	prime := 31
	result = 1
	result *= prime
	// TODO

	return
}

func (route *TopicRouteData) Equals(route1 interface{}) bool {
	if route == nil {
		return true
	}
	if route1 == nil {
		return false
	}
	//value, ok := route1.(TopicRouteData)
	//if !ok {
	//	return false
	//}
	// TODO
	//if route.brokerDatas == nil && value.brokerDatas != nil || len(route.brokerDatas) != len(value.brokerDatas) {
	//	return false
	//}
	//
	//if route.orderTopicConf == "" && value.orderTopicConf != "" || route.orderTopicConf != value.orderTopicConf {
	//	return false
	//}
	//
	//if route.queueDatas == nil && value.queueDatas != nil || route.queueDatas != value.queueDatas {
	//	return false
	//}
	//
	//if route.filterServerTable == nil && value.filterServerTable != nil ||
	//	route.filterServerTable != value.filterServerTable {
	//	return false
	//}
	return true
}

func (route *TopicRouteData) String() string {
	return fmt.Sprintf("TopicRouteData [orderTopicConf=%s, queueDatas=%s, brokerDatas=%s, filterServerTable=%s]",
		route.orderTopicConf, route.queueDatas, route.brokerDatas, route.filterServerTable)
}

type TopicPublishInfo struct {
	orderTopic         bool
	havaTopicRouteInfo bool
	messageQueueList   []*message.MessageQueue
	topicRouteData     *TopicRouteData
}

func (info *TopicPublishInfo) SetOrderTopic(b bool) {
	info.orderTopic = b
}

func (info *TopicPublishInfo) Ok() bool {
	return false
}

func (info *TopicPublishInfo) MessageQueueList() []*message.MessageQueue {
	return info.messageQueueList
}

func (info *TopicPublishInfo) HaveTopicRouteInfo() bool {
	return info.havaTopicRouteInfo
}

func (info *TopicPublishInfo) SetHaveTopicRouteInfo(b bool) {
	info.havaTopicRouteInfo = b
}

func (info *TopicPublishInfo) TopicRouteData() *TopicRouteData {
	return info.topicRouteData
}

func (info *TopicPublishInfo) SetTopicRouteData(routeDate *TopicRouteData) {
	info.topicRouteData = routeDate
}

func (info *TopicPublishInfo) SelectOneMessageQueue() *message.MessageQueue {
	return nil //TODo
}

func (info *TopicPublishInfo) selectOneMessageQueueWithBroker(brokerName string) *message.MessageQueue {
	if brokerName == "" {
		return info.SelectOneMessageQueue()
	}
	return nil //TODO
}

func (info *TopicPublishInfo) QueueIdByBroker(brokerName string) int {
	return nil //TODO
}

func (info *TopicPublishInfo) String() string {
	return nil
}
