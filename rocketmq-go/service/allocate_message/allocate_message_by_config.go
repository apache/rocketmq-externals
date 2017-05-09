package service_allocate_message

import ()
import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"

type AllocateMessageQueueByConfig struct {
	messageQueueList []model.MessageQueue
}

func (self *AllocateMessageQueueByConfig) Allocate(consumerGroup string, currentCID string, mqAll []*model.MessageQueue, cidAll []string) ([]model.MessageQueue, error) {
	return self.messageQueueList, nil
}
