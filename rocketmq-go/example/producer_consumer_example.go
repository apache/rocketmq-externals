package main

import ()
import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"net/http"
	_ "net/http/pprof"
	"time"
)

func main() {
	go func() {
		http.ListenAndServe("localhost:6060", nil)
	}()
	var (
		testTopic = "GoLang"
	)
	var producer1 = rocketmq.NewDefaultMQProducer("Test1")
	producer1.ProducerConfig.CompressMsgBodyOverHowMuch = 1
	var producer2 = rocketmq.NewDefaultMQProducer("Test2")
	var comsumer1 = rocketmq.NewDefaultMQPushConsumer(testTopic + "-StyleTang")
	comsumer1.ConsumerConfig.PullInterval = 0
	comsumer1.ConsumerConfig.ConsumeTimeout = 1
	comsumer1.ConsumerConfig.ConsumeMessageBatchMaxSize = 16
	comsumer1.ConsumerConfig.ConsumeFromWhere = "CONSUME_FROM_TIMESTAMP"
	comsumer1.ConsumerConfig.ConsumeTimestamp = time.Now()
	comsumer1.Subscribe(testTopic, "*")
	comsumer1.RegisterMessageListener(func(msgs []model.MessageExt) model.ConsumeConcurrentlyResult {
		for _, msg := range msgs {
			glog.Info(msg.BornTimestamp)
		}
		glog.Info("look message len ", len(msgs))
		return model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: model.CONSUME_SUCCESS, AckIndex: len(msgs)}
	})
	var clienConfig = &config.ClientConfig{}
	clienConfig.SetNameServerAddress("120.55.113.35:9876")
	rocketMqManager := rocketmq.MqClientManagerInit(clienConfig)
	rocketMqManager.RegistProducer(producer1)
	rocketMqManager.RegistProducer(producer2)
	rocketMqManager.RegistConsumer(comsumer1)
	rocketMqManager.Start()
	for i := 0; i < 1000; i++ {
		var message = &model.Message{}
		message.Topic = testTopic
		message.SetKeys([]string{"xxx"})
		message.SetTag("1122")
		message.Body = []byte("hellAXXWord" + util.IntToString(i))

		xx, ee := producer1.Send(message)
		if ee != nil {
			glog.Error(ee)
			continue
		}
		glog.V(0).Infof("sendMessageResutl messageId[%s] err[%s]", xx.MsgID(), ee)
	}
	select {}
	rocketMqManager.ShutDown()
}
