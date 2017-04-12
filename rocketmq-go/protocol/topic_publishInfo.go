package protocol

import (
	"../message"
	"container/list"
)

type TopicPublishInfo struct {
	orderTopic         bool
	havaTopicRouteInfo bool
	messageQueueList   *list.List
	topicRouteData     *TopicRouteData
}

func (info *TopicPublishInfo) SetOrderTopic(b bool) {
	info.orderTopic = b
}

func (info *TopicPublishInfo) Ok() bool {
	return false
}

func (info *TopicPublishInfo) MessageQueueList() *list.List {
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

func (info *TopicPublishInfo) SelectOneMessageQueue() *msg.MessageQueue {
	return nil //TODo
}

func (info *TopicPublishInfo) selectOneMessageQueueWithBroker(brokerName string) *msg.MessageQueue {
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
