package msg

type MessageQueue struct {
	topic      string
	brokerName string
	queueId    int32
}

func NewMessageQueue(topic string, brokerName string, queueId int32) *MessageQueue {
	return &MessageQueue{
		topic:      topic,
		brokerName: brokerName,
		queueId:    queueId,
	}
}

func (queue *MessageQueue) clone() *MessageQueue {
	no := new(MessageQueue)
	no.topic = queue.topic
	no.queueId = queue.queueId
	no.brokerName = queue.brokerName
	return no
}

func (queue MessageQueue) BrokerName() string {
	return queue.brokerName
}

func (queue *MessageQueue) QueueID() int32 {
	return queue.queueId
}

type MessageQueues []*MessageQueue

func (queues MessageQueues) Less(i, j int) bool {
	imq := queues[i]
	jmq := queues[j]

	if imq.topic < jmq.topic {
		return true
	} else if imq.topic < jmq.topic {
		return false
	}

	if imq.brokerName < jmq.brokerName {
		return true
	} else if imq.brokerName < jmq.brokerName {
		return false
	}

	if imq.queueId < jmq.queueId {
		return true
	}
	return false
}

func (queues MessageQueues) Swap(i, j int) {
	queues[i], queues[j] = queues[j], queues[i]
}

func (queues MessageQueues) Len() int {
	return len(queues)
}
