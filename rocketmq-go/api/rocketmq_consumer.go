package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type RocketMQConsumer interface {
	RegisterMessageListener(listener model.MessageListener)
	Subscribe(topic string, subExpression string)
}

func NewDefaultMQPushConsumer(producerGroup string) (r RocketMQConsumer) {
	return rocketmq.NewDefaultMQPushConsumer(producerGroup)
}
