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

![Go RocketMQ Client's Arch](http://www.plantuml.com/plantuml/svg/ZLNDRk8m4BxdANokL1w06ukWPTK4DW7j4RdEGACcDjWE4jjotvNsaNWdNc77Jan2C4jxg1ZFDn_V_C9ogXIHWhs3BhRBjUY5ss6U0TSXvNGDyYDG87SPWqHJ3S4XO5SHipBFGr0AuKF1jHHvRkBEs9gWDyXpmP2anlueeKHMYI_h_OUNkL8ofamBYP1YY_ogB43EwMiEQC-aI4lcxEuNXcTy9mknMYdG2RBwWXnf9uWS0dB5RmLYhjPRkoKKGcTytOlh3mv52CduJhn3ikUiiGRgmfno-4dtQ9-_xvfG50NbgM5afTAYU0QQWaHJmhO4fOSY1Mkf3LO0AjEZi5A6LLfbQwkFqm8IkHkagM53kF5FPfKwfIbd426aKkTdYoi9M4M6ZHRIyXej3B8TE8K4zTS94_q6f03z9j9UXNqQpPV3QPGimb6yJEQ3-I9PZkDfk1o-J4RHt0EA5r4qYPwMasWyhTozNaLsiFyxIXEG6SspNo2VKOSTLrBzVLU2Drxt6jkX3BSTpm4RRogcDfI16mip3NVNWpmyFjuUzJv38CM21pSXbg2PrQz7k9Le8XeM2-bEDsaZNVGD9Ba7dJQRzmLq3AyqDpFt1MpzM5SwgLxRSRZflcQPFfuJ4AOh_lLjgc0Ysdc8gguMPPsDx3kk5el7-xzxQU7-b4re0ZojdxzHzkxwMqFk14ylhcEhPguon5JjiZh4zg9pdwvH1jI-ENNMrqp-K-A3FzXqE-iSTBhTMehBZjU_SZPlxwBUH5_iLc0lWT1uoyI6YDMj8IBzq_6tEVUVTA_Ofx2d_dJyuuqbVGxwTFohN7Ru6OUG0pElxeww6tz1SRsPnHOteIW4ho_jFm00)

# How Go RocketMQ Client Works


1. create a new rocketMqMange instance(nameServerAddr ...)

2. create a new consumer instance(topic/tag/listener ...)(now only support cluster/concurrent)

3. consumer register to rocketMqMange

4. rocketMqMange start(StartAllScheduledTask )

* updateTopicRouteInfo
* heartbeat
* rebalance
* pullMessage
* cleanExpireMsg (Non-major)

## Scheduled Tasks

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
 
 ![pull-message-timing-diagram](http://www.plantuml.com/plantuml/svg/dPHHZzem3CVV-odynYO1Uwyc9AvNRfCet5hG7b7Qkb2HDbpY7BTlFoSK1bgPnDxYsFdPVyT9YhcGeYqGHfFql0v5HQX1Ntn7X2qI24Z4uMk2neWj_h1eSVYgLS6sDoP1UaM3LojbYcyM3KMg9QsaP6W8aDkwPDRXZnz4Mx9DGEfwsrE3Viu_4XntjKIGIXs0n1w1TWWrOGEg-elkdAtVxMJrfnjDdhJQemvB1KOrIBkwtOB85TTSINM4uXJwfJbHeDJgC1wFeQv0xOV2_6eBdmNE0PLM3UGU6fpOBAatTrW8FfUBOZ_cx4wC1saqLb8W9C5ikLuytokSryOssCdBKB_NVCF6varDdQyxTO_GNnL-O6495_X1Lm51RxhHP5uRmXPrmgrJPVYf2uizH6cMHyNETT7jUf5TepxpjCZoxEcmhWpE2xupj-ZWrhodNoDPtLuI6X9aJJ1mtOoMYsoTn9ji7KLnbWM3EvBwmS40fK58upDcFbt5AU-svRtUD6-HhB6bq72Grru9dk3oCYlkxeSyIOPgrkkSG_TOwfQVIsEsJ-oU-HF1GwNspG1KV1ctpCUW6XlrVhf1OmltDrnak4VklX7dOpm_nyeW3UsX58IT5VZkBPQRHVnpasGl3ytavN0oNKNVukV_12ndionURRxFv_7BTFuWe2r_0m00)
 
 ### cleanExpireMsg (Non-major)

when message cost too many time,we will drop this message（send message back） (for example 30 mins)



# Go RocketMQ Client's Roadmap
 [Go RocketMQ Client's Roadmap](https://github.com/StyleTang/incubator-rocketmq-externals/blob/master/rocketmq-go/docs/roadmap.md)

# Go RocketMQ Client's Check List

## todo