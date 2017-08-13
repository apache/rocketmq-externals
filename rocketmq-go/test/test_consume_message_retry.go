package main

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/golang/glog"
	"time"
)

//test consume message, first and second time consume error,third time consume success
func main() {
	chResult := make(chan bool, 3)
	var (
		nameServerAddress = "127.0.0.1:9876"
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestRetryProducerGroup"
		testConsumerGroup = "TestRetryConsumerGroup"
		tag               = "RetryTestTag"
		testMessageBody   = "RetryTestMessageBody"
		consumeTime       = 0
	)
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	var producer = rocketmq_api.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, tag)
	fmt.Println(tag)
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, message := range messageList {
			if string(message.Body) != testMessageBody {
				panic("message.Body is wrong message.Body=" + string(message.Body) + " testMessageBody=" + testMessageBody + " tag=" + message.GetTag())
			}
			if consumeTime < 2 {
				consumeTime++
				chResult <- true
				glog.Info("test consume fail")
				break
			}
			glog.Info("test consume success")
			chResult <- true
			successIndex = index
		}
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)
	rocketMQClientInstance.Start()
	var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte(testMessageBody)}
	message.SetTag(tag)
	result, err := producer.Send(message)
	glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	for i := 0; i < 3; i++ {
		select {
		case <-chResult:
		case <-time.After(time.Second * 50):
			panic("receive tag message timeout")
		}
	}
	glog.Info("Test tag message success")
}
