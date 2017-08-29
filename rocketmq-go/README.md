# RocketMQ Go SDK Millstone1 Detail Design[![Go Report Card](https://goreportcard.com/badge/github.com/StyleTang/incubator-rocketmq-externals)](https://goreportcard.com/report/github.com/StyleTang/incubator-rocketmq-externals)

## How to Use
examples:
* rocketmq-go/example/producer_consumer.go
* rocketmq-go/example/simple_consumer.go
* rocketmq-go/example/simple_producer_consumer.go

# Go RocketMQ Client's Arch

![Go RocketMQ Client's Arch](http://www.plantuml.com/plantuml/svg/bLNDRk8m4Bxp53vNAW_0ZMNxGzM4Dg7Q8tATWKOuDiGE4ajxtvNsaVfEVOMT-8UGO6YR0s8ytum_VvuPvq93EQSC25tb_UZnePHPnQ3SzNfllRpZ74cXgkAKa-SWz5WeJGK3X13x8RGdQ05gvR0xu-a647bvIPPG2k3zQ6rz4HLq0QN3Usg7o9ymfnNNZTFdTap3s4gzUC3Ny9plyNWQngbI9xZkUdOzrLG3WblOEb44IY4Gy-bISkx4RC66d4k6uGxwM445gIvnPR7TTS5szslAj2pHMwmzqobzve2sLyRQdUYW9hipg4O9UurQacSWcHHdKkzBYhprI52ykbgEVFRqzxdI87oqWvHGI9sBHJznVtmQP9IBZLn2kXMiiOQgPKu4_A9RjFTx7YUKHK55LcTabTAok0MQcN8fOLs1qYEPWR7K2WK1QmOOMcOesZnV67vrBwJbPfXbflYOxO87oyxsGemGoowSBR4KK5Y68abt5OXYa6_0CoJ8tnsOw1SW1UWNcVKjzkPgEXjDulGfkafcwNGuVKfdryaacetknrVnD01Adk9u7D_Cnl7rnBhxVPlipVnscAPGvfJdlw6yHHxSrmj-TqgAnkjN4ycMeADrnuFjTZ0zR6bWjVJETHIvQ9gurHu3XXYAAjkkRtUkQVrWlfFeLdA32krgw0GRhStdAx5D-xLZW-aFTV3qoO1yBt0odolGAPA1RyScy4ov6UxVZNHhxYgm9ZRvSPOl2EP6JoDMPtl_f0ILQeBQRvYlmiEoCFLmOYorh-fGq3h8Pp3EHh5FN0TP0jvpBuY1X_MjGrCGcLl-GTdIAsJF_4bibzHfP6Uuj_NrCf9QuhQKnDnKPqWVeoxspMa7hvFeE5DudeRW5qQRpaopbO7SfSOPArT4payL0HbylF-fdw5NkbNiLPedwEFzRp2M30JeZ_T_oc-8_x681B1wrSzt__Ry1m00)

# Go RocketMQ Client's Roadmap
 [Go RocketMQ Client's Roadmap](https://github.com/StyleTang/incubator-rocketmq-externals/blob/master/rocketmq-go/docs/roadmap.md)

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



