package model

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"

//MessageQueues queue array
type MessageQueues []*rocketmqm.MessageQueue

//Less compare queue
func (m MessageQueues) Less(i, j int) bool {
	imq := m[i]
	jmq := m[j]

	if imq.Topic < jmq.Topic {
		return true
	} else if imq.Topic < jmq.Topic {
		return false
	}

	if imq.BrokerName < jmq.BrokerName {
		return true
	} else if imq.BrokerName < jmq.BrokerName {
		return false
	}
	return imq.QueueId < jmq.QueueId
}

//Swap swap queue
func (m MessageQueues) Swap(i, j int) {
	m[i], m[j] = m[j], m[i]
}

//Len messageQueues's length
func (m MessageQueues) Len() int {
	return len(m)
}
