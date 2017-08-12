package rocketmq_api

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/manage"
)

type RocketMQClientInstance interface {
	RegisterProducer(producer RocketMQProducer)
	RegisterConsumer(consumer RocketMQConsumer)
	Start()
}

type RocketMQClientInstanceImpl struct {
	rocketMqManager *rocketmq.MqClientManager
}

func InitRocketMQClientInstance(nameServerAddress string) (rocketMQClientInstance RocketMQClientInstance) {
	var mqClientConfig = &rocketmq_api_model.MqClientConfig{NameServerAddress: nameServerAddress}
	rocketMQClientInstance = &RocketMQClientInstanceImpl{rocketMqManager: rocketmq.MqClientManagerInit(mqClientConfig)}
	return
}

func (self *RocketMQClientInstanceImpl) RegisterProducer(producer RocketMQProducer) {
	self.rocketMqManager.RegistProducer(producer.(*rocketmq.DefaultMQProducer))
}

func (self *RocketMQClientInstanceImpl) RegisterConsumer(consumer RocketMQConsumer) {
	self.rocketMqManager.RegistConsumer(consumer.(*rocketmq.DefaultMQPushConsumer))
}
func (self *RocketMQClientInstanceImpl) Start() {
	self.rocketMqManager.Start()
}
