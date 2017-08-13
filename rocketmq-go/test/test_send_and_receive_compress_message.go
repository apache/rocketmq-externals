package main

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/golang/glog"
)

func main() {
	chResult := make(chan bool, 1)
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestCompressProducerGroup"
		testConsumerGroup = "TestCompressConsumerGroup"
	)
	var bigMessageBody = "test_string"
	for i := 0; i < 16; i++ {
		bigMessageBody += bigMessageBody
	}
	//bigMessageBody len will be 720896,it will be compressed
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	producerConfig := rocketmq_api_model.NewProducerConfig()
	producerConfig.CompressMsgBodyOverHowMuch = 500
	var producer = rocketmq_api.NewDefaultMQProducerWithCustomConfig(testProducerGroup, producerConfig)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, "compress_message_test")
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			if msg.SysFlag&constant.CompressedFlag != constant.CompressedFlag {
				panic("message not be compressed")
			}
			if string(msg.Body) != bigMessageBody {
				panic("message not be unCompressed")
			}
			glog.Info("Test compress and tag success")
			successIndex = index

		}
		chResult <- true
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte(bigMessageBody)}
	message.SetTag("compress_message_test")
	result, err := producer.Send(message)
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	<-chResult
}
