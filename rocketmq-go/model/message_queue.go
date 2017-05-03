package model

type MessageQueue struct {
	Topic      string `json:"topic"`
	BrokerName string `json:"brokerName"`
	QueueId    int32  `json:"queueId"`
}

func (self *MessageQueue) clone() *MessageQueue {
	no := new(MessageQueue)
	no.Topic = self.Topic
	no.QueueId = self.QueueId
	no.BrokerName = self.BrokerName
	return no
}

type MessageQueues []*MessageQueue

func (self MessageQueues) Less(i, j int) bool {
	imq := self[i]
	jmq := self[j]

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

	if imq.QueueId < jmq.QueueId {
		return true
	} else {
		return false
	}
}

func (self MessageQueues) Swap(i, j int) {
	self[i], self[j] = self[j], self[i]
}

func (self MessageQueues) Len() int {
	return len(self)
}

func (self MessageQueue) Equals(messageQueue *MessageQueue) bool {
	if self.QueueId != messageQueue.QueueId {
		return false
	}
	if self.Topic != messageQueue.Topic {
		return false
	}
	if self.BrokerName != messageQueue.BrokerName {
		return false
	}
	return true
}
