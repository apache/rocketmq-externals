package main

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/golang/glog"
	"time"
)

func main() {
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestSerializeProducerGroup"
		testConsumerGroup = "TestSerializeConsumerGroup"
		tag               = "TestSerializeMessageTag"
		messageBody       = "testMessageBody_testMessageBody"
		messageCount      = 100
	)
	chResult := make(chan bool, messageCount)
	mqClientConfig := rocketmq_api_model.NewMqClientConfig(nameServerAddress)
	mqClientConfig.ClientSerializeType = rocketmq_api_model.ROCKETMQ_SERIALIZE
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstanceWithCustomClientConfig(mqClientConfig)
	var producer = rocketmq_api.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			if msg.GetTag() == tag && messageBody == string(messageBody) {
				chResult <- true
			}
			successIndex = index

		}
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	for i := 0; i < messageCount; i++ {
		var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte(messageBody)}
		message.SetTag(tag)
		result, err := producer.Send(message)
		glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	}
	for i := 0; i < messageCount; i++ {
		select {
		case <-chResult:
		case <-time.After(time.Second * 30):
			panic("receive tag message timeout")
		}
	}
	glog.Info("Test tag message success")

}
