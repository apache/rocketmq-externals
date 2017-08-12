package rocketmq

import ()
import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
)

type DefaultMQProducer struct {
	producerGroup  string
	ProducerConfig *rocketmq_api_model.RocketMqProducerConfig

	producerService service.ProducerService
}

func NewDefaultMQProducer(producerGroup string, producerConfig *rocketmq_api_model.RocketMqProducerConfig) (rocketMQProducer *DefaultMQProducer) {
	rocketMQProducer = &DefaultMQProducer{
		producerGroup:  producerGroup,
		ProducerConfig: producerConfig,
	}
	return
}

func (self *DefaultMQProducer) Send(message *rocketmq_api_model.Message) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", self.ProducerConfig.SendMsgTimeout)
	return
}
func (self *DefaultMQProducer) SendWithTimeout(message *rocketmq_api_model.Message, timeout int64) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", timeout)
	return
}
