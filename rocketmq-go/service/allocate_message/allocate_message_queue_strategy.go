package service_allocate_message

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"

type AllocateMessageQueueStrategy interface {
	Allocate(consumerGroup string, currentCID string, mqAll []*model.MessageQueue, cidAll []string) ([]model.MessageQueue, error)
}

func GetAllocateMessageQueueStrategyByConfig(allocateMessageQueueStrategy string) AllocateMessageQueueStrategy {
	return new(AllocateMessageQueueAveragely)
	//switch allocateMessageQueueStrategy {
	//
	//}
	//
	//return -1

}
