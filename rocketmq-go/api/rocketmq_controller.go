package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
)

type RocketMQController struct {
	rocketMqManager *rocketmq.MqClientManager
}

func InitRocketMQController(clientConfig *rocketmq_api_model.ClientConfig) (rocketMQController *RocketMQController) {
	rocketMQController = &RocketMQController{}
	rocketMQController.rocketMqManager = rocketmq.MqClientManagerInit(clientConfig)
	return

}
func (self *RocketMQController) RegistProducer(producer RocketMQProducer) {
	self.rocketMqManager.RegistProducer(producer.(*rocketmq.DefaultMQProducer))
}

func (self *RocketMQController) RegistConsumer(consumer RocketMQConsumer) {
	self.rocketMqManager.RegistConsumer(consumer.(*rocketmq.DefaultMQPushConsumer))
}
func (self *RocketMQController) Start() {
	self.rocketMqManager.Start()
}

