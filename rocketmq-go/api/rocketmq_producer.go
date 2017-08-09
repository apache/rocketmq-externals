package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type RocketMQProducer interface {
	Send(message *rocketmq_api_model.Message) (sendResult *model.SendResult, err error)
	SendWithTimeout(message *rocketmq_api_model.Message, timeout int64) (sendResult *model.SendResult, err error)
}

func NewDefaultMQProducer(producerGroup string) (r RocketMQProducer) {
	return rocketmq.NewDefaultMQProducer(producerGroup)
}
