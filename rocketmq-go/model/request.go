package rocketmq

type SendMessageRequestHeader struct {
	CommandCustomHeader
	producerGroup        string
	topic                string
	dafaultTopic         string
	defaultTopicQueueNum int
	queueID              int
	sysFlag              int
	bornTimestamp        int
	flag                 int
	properties           string
	reconsumeTimes       int
	unitMode             bool
	maxReconsumeTimes    int
}
