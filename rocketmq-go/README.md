# RocketMQ Go SDK Millstone1 Detail Design

## Example 
```
func main() {

	// create a mqClientManager instance
	var mqClientConfig = &rocketmq.MqClientConfig{}
	var mqClientManager = rocketmq.NewMqClientManager(mqClientConfig)

	// create rocketMq consumer
	var consumerConfig = &rocketmq.MqConsumerConfig{}
	var consumer1 = rocketmq.NewDefaultMQPushConsumer("testGroup", consumerConfig)
	consumer1.Subscribe("testTopic", "*")
	consumer1.RegisterMessageListener(func(msgs []model.MessageExt) model.ConsumeConcurrentlyResult {
		var index = -1
		for i, msg := range msgs {
			// your code here,for example,print msg
			glog.Info(msg)
			var err = errors.New("error")
			if err != nil {
				break
			}
			index = i
		}
		return model.ConsumeConcurrentlyResult{ConsumeConcurrentlyStatus: model.CONSUME_SUCCESS, AckIndex: index}
	})

	//register consumer to mqClientManager
	mqClientManager.RegisterConsumer(consumer1)

	//start it
	mqClientManager.Start()

}
```

# Go RocketMQ Client's Arch

![Go RocketMQ Client's Arch](http://www.plantuml.com/plantuml/svg/ZLNDRk8m4BxdANokL1w06ukWPRL9K0Bg4RdEG2CI6so3nBRSTwLz9Dw9LzYn4pU_23YWZEURoJUVPoOieqeHATWMTBvVRSJ0tu-p5BZsAQSRa9-U0dbC6SIQQl3E7kj4p0zPveDI21yBhgN8CXFNYoroJodJGfu-lNoF4UgGWrpGzmpGdakH79YpNOgIWo5Nubkyn7gjGCUOjI_89Mq5aRD7ElajGMHllQLRGYvqoZU3g7nmA498oezY2_69iyeQgOOPSdYZ9xI_tzTDAFASyiHaP0UbHVuAD069feJz0PGUYmIifNQe01Hc3yE26QgqJZUM7wSvnF8uIXAJ3U76FviNx9Icd460a89TVxQee80gChnc8MrUmuL1UWJ7a23_ic2YFu3ae3z4CYYmBvF5QXm6yTAVHAjuCLeiuzKq2ltL-DTi4YnwGD4o26R1oseMJCF2FHWKoSxslyKq1fdIBFqDyXLvs0OanU-d4hpokjF8DgM_RtdEUF56CRMZ35nRc6wus_3awVdvTgiF6G8f2kxTXDW4LQy-7k56eegeibX8kxj97UcY6qXJuMW-UvDqjqFNaDmB2AlvUm0TmJlDJESzW2qFIxtZuhGUX_FUTsNcDbt1M0eHvdhwvxYImmJLIn65m1PbdIrXElvlYUiU3B3EgknENDDLes2z_HeOP0myAOz_KBPrrrv2xWI7ZQLP4z7fH8oCJQs8lPpMskgY3AY-YieYkvdwDqmV_CcMsvfohtNzjLbfja3vERDrzyxlJNHZv3ox88J65ITsHAnljHGPNYz_p5VWYvuK-nBs2t2z_EsD9KCE-dhvfrnr-8mjoO6PKxTUtUh_3m00)

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