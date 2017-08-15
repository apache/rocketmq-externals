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
		testProducerGroup = "TestDelayProducerGroup"
		testConsumerGroup = "TestDelayConsumerGroup"
		tag               = "TestDelayMessageTag"
	)
	var messageId string
	var startTime time.Time
	chResult := make(chan bool, 1)
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	var producer = rocketmq_api.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			endTime := time.Now()
			if msg.MsgId != messageId {
				panic("messageId is wrong " + msg.MsgId)
			}
			costSeconds := endTime.Unix() - startTime.Unix()
			if costSeconds < 14 || costSeconds > 16 {
				panic("delay time message is error ")
			}
			chResult <- true
			successIndex = index

		}
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	<-time.After(time.Second * 30) // wait
	var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte("hello world")}
	message.SetTag(tag)
	message.SetDelayTimeLevel(3) // cost 15 second
	result, err := producer.Send(message)
	startTime = time.Now()
	messageId = result.MsgID()
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	select {
	case <-chResult:
	case <-time.After(time.Second * 30):
		panic("receive tag message timeout")
	}
	glog.Info("Test tag message success")

}
