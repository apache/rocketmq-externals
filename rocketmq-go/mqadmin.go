package rocketmq

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/message"

type MQAdmin interface {
	CreateTopic(key, topicName string, queueNum int) error
	CreateTopicWithSysFlag(key, topicName string, queueNum, sysFlag int) error
	SearchOffset(mq msg.MessageQueue, timeStamp int64) error
	MaxOffset(mq msg.MessageQueue) error
	MinOffset(mq msg.MessageQueue) error
	EarliestMsgStoreTime(mq msg.MessageQueue) error
	// TODO confirm method use
	ViewMessage(topic string, msgId string) (msg.MessageExt, error)
	ViewMessageByOffsetMsgId(offsetMsgId string) (msg.MessageExt, error)
	QueryMessage(key, topic string, maxNum int, begin, end int64) (QueryResult, error)
}
