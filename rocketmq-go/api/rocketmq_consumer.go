package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type RocketMQConsumer interface {
	RegisterMessageListener(listener model.MessageListener)
	Subscribe(topic string, subExpression string)
}

func NewDefaultMQPushConsumer(producerGroup string) (r RocketMQConsumer) {
	return rocketmq.NewDefaultMQPushConsumer(producerGroup, rocketmq_api_model.NewRocketMqConsumerConfig())
}

func NewDefaultMQPushConsumerWithCustomConfig(producerGroup string, consumerConfig *rocketmq_api_model.RocketMqConsumerConfig) (r RocketMQConsumer) {
	return rocketmq.NewDefaultMQPushConsumer(producerGroup, consumerConfig)
}
