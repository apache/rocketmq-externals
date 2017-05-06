# How Go RocketMQ Client Works


1. create a new rocketMqMange instance(nameServerAddr ...)

2. create a new consumer instance(topic/tag/listener ...)(now only support cluster/concurrent)

3. consumer register to rocketMqMange

4. rocketMqMange start(StartAllScheduledTask )

## Scheduled Tasks

 * updateTopicRouteInfo
 
 update Topic Route Info by consumer subscription data (topic route info data get from name server) 
 
 put them into local memory(BrokerAddrTable/TopicPublishInfoTable/TopicSubscribeInfoTable/TopicRouteTable)

![update-topic-routeInfo-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=)

 * heartbeat:
 
 prepare heartbeat data(all consumer and producer data in this client)
 
 send it to all brokers.(broker data is from BrokerAddrTable) 
  
 (only broker know the distribution of the consumers we can rebalance)

![heartbeat-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/go-client-detail-design/rocketmq-go/docs/heartbeat-timing-diagram.puml)

 * rebalance
 
 for each MqClientManager.ClientFactory's consumers,invoke consumer.rebalance's DoRebalance method
 
 (after rebalance we can know the (topic/consumer group) should consume from which broker which queue)
 
  put them into local memory(processQueueTable)
  
  enqueue pull message request (chan *model.PullRequest)
  
 ![rebalance-timing-diagram](http://www.plantuml.com/plantuml/svg/XL7DQiCm3BxdANJSO7s170gZi55OsTOMTdPi9J4uaclBbBtz72TBCoZiPblVdpuViL5EaKROR8-_vxhb0AXq3yBUQh04fzH47QmNorGjmCtsSDavYoHrQydic68QCEpDcutoKCXFNU3a7-zoNb7E8sOMRt1FBK-qFuHdvrWhmGF6g3hyJ9Zm926_TD-rceTmjT93le6UOu0kDZ260KMc32yZEM_KyjhXjdho9ejz1DRPh3YTLUDoiWLIAIUmk2eWlCwg3Jgc3gItSGcnTdblsuXo4WvOQnvyoaR9kPV0mrUF0QiLO5LJG6M0o-XkZKYJlSzQC4mTGS3y6AL25n76pyb99nYn_9lqreT1XtdDWlIhLYeaymC0)
  
 * pullMessage
 
 dequeue pull message request and pull message from broker,when get messages to consume,
 put them into consume request,consume request handler will call the listener consume the message
 
 enqueue a new pull message request and commit our consume offset to broker
 
 ![pull-message-timing-diagram](http://www.plantuml.com/plantuml/svg/dPHHZzem3CVV-odynYO1Uwyc9AvNRfCet5hG7b7Qkb2HDbpY7BTlFoSK1bgPnDxYsFdPVyT9YhcGeYqGHfFql0v5HQX1Ntn7X2qI24Z4uMk2neWj_h1eSVYgLS6sDoP1UaM3LojbYcyM3KMg9QsaP6W8aDkwPDRXZnz4Mx9DGEfwsrE3Viu_4XntjKIGIXs0n1w1TWWrOGEg-elkdAtVxMJrfnjDdhJQemvB1KOrIBkwtOB85TTSINM4uXJwfJbHeDJgC1wFeQv0xOV2_6eBdmNE0PLM3UGU6fpOBAatTrW8FfUBOZ_cx4wC1saqLb8W9C5ikLuytokSryOssCdBKB_NVCF6varDdQyxTO_GNnL-O6495_X1Lm51RxhHP5uRmXPrmgrJPVYf2uizH6cMHyNETT7jUf5TepxpjCZoxEcmhWpE2xupj-ZWrhodNoDPtLuI6X9aJJ1mtOoMYsoTn9ji7KLnbWM3EvBwmS40fK58upDcFbt5AU-svRtUD6-HhB6bq72Grru9dk3oCYlkxeSyIOPgrkkSG_TOwfQVIsEsJ-oU-HF1GwNspG1KV1ctpCUW6XlrVhf1OmltDrnak4VklX7dOpm_nyeW3UsX58IT5VZkBPQRHVnpasGl3ytavN0oNKNVukV_12ndionURRxFv_7BTFuWe2r_0m00)
 
 * cleanExpireMsg (Non-major)

when message cost too many time,we will drop this message（send message back） (for example 30 mins)



