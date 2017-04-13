package rocketmq

import (
	"./message"
	"fmt"
)

type BrokerData struct {
}

type TopicRouteData struct {
	orderTopicConf    string
	queueDatas        []*msg.MessageQueue
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

func (route *TopicRouteData) QueueDatas() []*msg.MessageQueue {
	return route.queueDatas
}

func (route *TopicRouteData) SetQueueDatas(data []*msg.MessageQueue) {
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
