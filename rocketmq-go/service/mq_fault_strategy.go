package service

import (
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

//发送MQ故障时的策略 这个功能 默认是关闭的 我们晚些时候再实现它
type MQFaultStrategy struct {
}

//if first select : random one
//if has error broker before ,skip the err broker
func selectOneMessageQueue(topicPublishInfo *model.TopicPublishInfo, lastFailedBroker string) (mqQueue model.MessageQueue, err error) {
	queueIndex := topicPublishInfo.FetchQueueIndex()
	queues := topicPublishInfo.MessageQueueList
	if len(lastFailedBroker) == 0 {
		mqQueue = queues[queueIndex]
		return
	}
	for i := 0; i < len(queues); i++ {
		nowQueueIndex := queueIndex + i
		if nowQueueIndex >= len(queues) {
			nowQueueIndex = nowQueueIndex - len(queues)
		}
		if lastFailedBroker == queues[nowQueueIndex].BrokerName {
			continue
		}
		mqQueue = queues[nowQueueIndex]
		return
	}
	err = errors.New("send to [" + lastFailedBroker + "] fail,no other broker")
	return
}
