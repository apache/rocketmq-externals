package rocketmq

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"time"
)

type CleanExpireMsgController struct {
	mqClient      service.RocketMqClient
	clientFactory *ClientFactory
}

func NewCleanExpireMsgController(mqClient service.RocketMqClient, clientFactory *ClientFactory) *CleanExpireMsgController {
	return &CleanExpireMsgController{
		mqClient:      mqClient,
		clientFactory: clientFactory,
	}
}

func (self *CleanExpireMsgController) Start() {
	for _, consumer := range self.clientFactory.ConsumerTable {
		go func() {
			cleanExpireMsgTimer := time.NewTimer(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * 1000 * 60 * time.Millisecond)
			//cleanExpireMsgTimer := time.NewTimer(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * time.Millisecond)
			for {
				<-cleanExpireMsgTimer.C
				consumer.CleanExpireMsg()
				cleanExpireMsgTimer.Reset(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * 1000 * 60 * time.Millisecond)
				//cleanExpireMsgTimer.Reset(time.Duration(consumer.ConsumerConfig.ConsumeTimeout) * time.Millisecond)
			}
		}()
	}
}
