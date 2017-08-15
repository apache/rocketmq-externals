# RocketMQ Go SDK Millstone1 Detail Design

## Example 
```
func main() {
	var (
		nameServerAddress = "127.0.0.1:9876" //address split by ;  (for example 192.168.1.1:9876;192.168.1.2:9876)
		testTopic         = "GoLangRocketMQ"
		testProducerGroup = "TestProducerGroup"
		testConsumerGroup = "TestConsumerGroup"
	)
	// init rocketMQClientInstance
	rocketMQClientInstance := rocketmq_api.InitRocketMQClientInstance(nameServerAddress)
	// init rocketMQProducer and register it
	var producer = rocketmq_api.NewDefaultMQProducer(testProducerGroup)
	rocketMQClientInstance.RegisterProducer(producer)

	// 1.init rocketMQConsumer
	// 2.subscribe topic and register our function to message listener
	// 3.register it
	var consumer = rocketmq_api.NewDefaultMQPushConsumer(testConsumerGroup)
	consumer.Subscribe(testTopic, "*")
	consumer.RegisterMessageListener(func(messageList []rocketmq_api_model.MessageExt) rocketmq_api_model.ConsumeConcurrentlyResult {
		successIndex := -1
		for index, msg := range messageList {
			glog.Infof("test receiveMessage messageId=[%s] messageBody=[%s]", msg.MsgId, string(msg.Body))
			// call your function
			successIndex = index
		}
		return rocketmq_api_model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: rocketmq_api_model.CONSUME_SUCCESS, AckIndex: successIndex}
	})
	rocketMQClientInstance.RegisterConsumer(consumer)

	// start rocketMQ client instance
	rocketMQClientInstance.Start()

	//start send test message
	for {
		var message = &rocketmq_api_model.Message{Topic: testTopic, Body: []byte("hello World")}
		result, err := producer.Send(message)
		glog.Infof("test sendMessageResult messageId=[%s] err=[%s]", result.MsgID(), err)
	}

```

# Go RocketMQ Client's Arch

![Go RocketMQ Client's Arch](http://www.plantuml.com/plantuml/svg/ZLNDRk8m4BxdANokL1w06qNzOTK4Dg7g4RdEGACcDjWE4jjotvNsaVfEVOKTsp7vPIa7bF5yy-MRySo4vLGf8WLx0rtiLXin2dVJF0EkGyhf1kHxe43kCmQ9fXg2Oy1w4OiopqDG2k5JmRKKUMxYJjYAO3J9Sy6GfCB-BA54LeadcyFx8BDJSaUP5X8XnGxvLLc0NDAN7D1UI96MpDlT5uOd_9aiQg0dwW39CI0zJYiAynjmkimUCwM16p83gJ0I2g4plXd5rOFd8OHaV2_U83bmLbiJrJBd79xf0UtJI_k4eYWeJmqZAvKMnGFG52IQ4dObA3qLALXBRR4kuCm1XKuPrcwTRQm-JWj8v7wIfeODuSO_MpXrIbFE84A8KpVC5Zi9M6U6HHBIyXej3B8zU8K4zLS94_qAf03zAjAHmzxVBbVJUPGyXRVnAbbEba_9wYwUXwlfu-msMWw0ugSecaNtgrbqDtVkqSYH7Ur_Hsa2CgDvzWla0-gmJKpSxuSIlFRwqzYsQhZhU8v149YAgIrbu7i3-oMuE3Jawlhw_02C6G8f5Zmu2x44TJT_Fy8FIXrHirX8j_z94-cZMqYJuMGnUvPqkqNNc5mAcA_N2dI2gk0Rw1XUQ6uwxnlOwh2gT-9Ect6u4OQkpDocJnXJ8S9Sp_0Sr-KmGSrKX2kmHLbdfp0z_wd4JGy6N0hsaBeoFUgsgw5omFk_TMtXmrKreGdmj3g-eUpDDr85kH3SLNKUsLctxKlqzqJCDRRlYjZcwApDFig0cgkmgb6VQNZ33S6lR74wMoRej5zZLjijM7sIRUw3--CikZNoWLrGGkEA5LiYrkTQceo_Fd-Rhz07FIZs8UmNwElpps0i6NFGNvz_bSyEdt4aE31pwjUx__Jy0m00)

# How Go RocketMQ Client Works


1. create a new rocketMqMange instance(nameServerAddr ...)

2. create a new consumer instance(topic/tag/listener ...)(now only support cluster/concurrent)

3. consumer register to rocketMqMange
 
4. rocketMqMange start

* register ClientRequestProcessor
  * CHECK_TRANSACTION_STATE
  * NOTIFY_CONSUMER_IDS_CHANGED
  * RESET_CONSUMER_CLIENT_OFFSET
  * GET_CONSUMER_STATUS_FROM_CLIENT
  * GET_CONSUMER_RUNNING_INFO
  * CONSUME_MESSAGE_DIRECTLY
 
* Start All Task
    * updateTopicRouteInfo
    * heartbeat
    * rebalance
    * pullMessage
    * cleanExpireMsg (Non-major)

## All Tasks

 ### updateTopicRouteInfo
 
 update Topic Route Info by consumer subscription data (topic route info data get from name server) 
 
 put them into local memory(BrokerAddrTable/TopicPublishInfoTable/TopicSubscribeInfoTable/TopicRouteTable)

![update-topic-routeInfo-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/go-client-detail-design/rocketmq-go/docs/update-topic-routeInfo-timing-diagram.puml)

 ### heartbeat:
 
 prepare heartbeat data(all consumer and producer data in this client)
 
 send it to all brokers.(broker data is from BrokerAddrTable) 
  
 (only broker know the distribution of the consumers we can rebalance)

![heartbeat-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/go-client-detail-design/rocketmq-go/docs/heartbeat-timing-diagram.puml)

 ### rebalance
 
 for each MqClientManager.ClientFactory's consumers,invoke consumer.rebalance's DoRebalance method
 
 (after rebalance we can know the (topic/consumer group) should consume from which broker which queue)
 
  put them into local memory(processQueueTable)
  
  enqueue pull message request (chan *model.PullRequest)
  
 ![rebalance-timing-diagram](http://www.plantuml.com/plantuml/svg/XL7DQiCm3BxdANJSO7s170gZi55OsTOMTdPi9J4uaclBbBtz72TBCoZiPblVdpuViL5EaKROR8-_vxhb0AXq3yBUQh04fzH47QmNorGjmCtsSDavYoHrQydic68QCEpDcutoKCXFNU3a7-zoNb7E8sOMRt1FBK-qFuHdvrWhmGF6g3hyJ9Zm926_TD-rceTmjT93le6UOu0kDZ260KMc32yZEM_KyjhXjdho9ejz1DRPh3YTLUDoiWLIAIUmk2eWlCwg3Jgc3gItSGcnTdblsuXo4WvOQnvyoaR9kPV0mrUF0QiLO5LJG6M0o-XkZKYJlSzQC4mTGS3y6AL25n76pyb99nYn_9lqreT1XtdDWlIhLYeaymC0)
  
 ### pullMessage
 
 dequeue pull message request and pull message from broker,when get messages to consume,
 put them into consume request,consume request handler will call the listener consume the message
 
 enqueue a new pull message request and commit our consume offset to broker
 
 #### pullMessage
 
 ![pull-message-timing-diagram](http://www.plantuml.com/plantuml/svg/dPHHZzem3CVV-odynYO1Uwyc9AvNRfCet5hG7b7Qkb2HDbpY7BTlFoSK1bgPnDxYsFdPVyT9YhcGeYqGHfFql0v5HQX1Ntn7X2qI24Z4uMk2neWj_h1eSVYgLS6sDoP1UaM3LojbYcyM3KMg9QsaP6W8aDkwPDRXZnz4Mx9DGEfwsrE3Viu_4XntjKIGIXs0n1w1TWWrOGEg-elkdAtVxMJrfnjDdhJQemvB1KOrIBkwtOB85TTSINM4uXJwfJbHeDJgC1wFeQv0xOV2_6eBdmNE0PLM3UGU6fpOBAatTrW8FfUBOZ_cx4wC1saqLb8W9C5ikLuytokSryOssCdBKB_NVCF6varDdQyxTO_GNnL-O6495_X1Lm51RxhHP5uRmXPrmgrJPVYf2uizH6cMHyNETT7jUf5TepxpjCZoxEcmhWpE2xupj-ZWrhodNoDPtLuI6X9aJJ1mtOoMYsoTn9ji7KLnbWM3EvBwmS40fK58upDcFbt5AU-svRtUD6-HhB6bq72Grru9dk3oCYlkxeSyIOPgrkkSG_TOwfQVIsEsJ-oU-HF1GwNspG1KV1ctpCUW6XlrVhf1OmltDrnak4VklX7dOpm_nyeW3UsX58IT5VZkBPQRHVnpasGl3ytavN0oNKNVukV_12ndionURRxFv_7BTFuWe2r_0m00)
 
 #### consumeMessage(submitConsumeRequest)
 
 ![consume-message-activity](http://www.plantuml.com/plantuml/svg/VL5FQy8m5B_tKxoR4PdWjKvaAnu4Ey7Eqv0HsnSDQvCkBztcjzzBxT3si1m2UVdzJSXBvwEuGY9vmeqc3mlmJXfIrbNfTVng4skegN0UnGu38htXqQ0JT_pnFD8A1EEc7IlpqZS4YmMCakrBjazNxza-ILPPDbgEmP_HWBWWZIE0MEOVQrFW3ti4XQVkE8-m90HXx13rCECxKsWLnTJaEUVeih4XL7IYjnjw0hC3Lm3DLt_32VE_pxfaSGsFBMDQeZdvyxAr8XP_Pl0EgIb3zJ3eBC9Sj1xwmBK1j19z_B3VTGsJJcLTCwqd5VhU24fomk9VVFi6lBTbeZYtLQNzLYbgFynXPRymw_cAGGpMuMH7fdKVjyFF1icBldk0DNKXFLxLrsPZSrccxE0kujwNaUHj_Gi0)
   
 ### cleanExpireMsg (Non-major)

when message cost too many time,we will drop this message（send message back） (for example 30 mins)



# Go RocketMQ Client's Roadmap
 [Go RocketMQ Client's Roadmap](https://github.com/StyleTang/incubator-rocketmq-externals/blob/master/rocketmq-go/docs/roadmap.md)

# Go RocketMQ Client's Check List

## todo