package client

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/producer"
)

type MQClientInstance struct {
}

func NewMQClientInstance(config *ClientConfig, instanceIndex int, id string /*hookRPCHook*/) *MQClientInstance {
	return &MQClientInstance{}
}

func (instance *MQClientInstance) RegisterProducer(group string, p *producer.DefaultProducer) bool {
	//TODO
	return true
}

func (instance *MQClientInstance) Start() {

}

func (instance *MQClientInstance) FindBrokerAddressInPublish(name string) string {
	return nil
}

func (instance *MQClientInstance) MQClientAPI() *MQClientAPI {
	return nil
}
