# RocketMQ Go SDK Millstone1 Detail Design[![Go Report Card](https://goreportcard.com/badge/github.com/StyleTang/incubator-rocketmq-externals)](https://goreportcard.com/report/github.com/StyleTang/incubator-rocketmq-externals)

## How to Use
examples:
* rocketmq-go/example/producer_consumer.go
* rocketmq-go/example/simple_consumer.go
* rocketmq-go/example/simple_producer_consumer.go

# Go RocketMQ Client's Arch

![Go RocketMQ Client's Arch](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/package.puml)

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

![update-topic-routeInfo-timing-diagram](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/update-topic-routeInfo-timing-diagram.puml)

 ### heartbeat:
 
 prepare heartbeat data(all consumer and producer data in this client)
 
 send it to all brokers.(broker data is from BrokerAddrTable) 
  
 (only broker know the distribution of the consumers we can rebalance)

![heartbeat-timing-diagram](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/heartbeat-timing-diagram.puml)

 ### rebalance
 
 for each MqClientManager.ClientFactory's consumers,invoke consumer.rebalance's DoRebalance method
 
 (after rebalance we can know the (topic/consumer group) should consume from which broker which queue)
 
  put them into local memory(processQueueTable)
  
  enqueue pull message request (chan *model.PullRequest)
  
 ![rebalance-timing-diagram](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/rebalance-timing-diagram.puml)
  
 ### pullMessage
 
 dequeue pull message request and pull message from broker,when get messages to consume,
 put them into consume request,consume request handler will call the listener consume the message
 
 enqueue a new pull message request and commit our consume offset to broker
 
 #### pullMessage
 
 ![pull-message-timing-diagram](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/pull-message-timing-diagram.puml)
 
 #### consumeMessage(submitConsumeRequest)
 
 ![consume-message-activity](http://www.plantuml.com/plantuml/proxy?fmt=svg&src=https://raw.githubusercontent.com/StyleTang/incubator-rocketmq-externals/master/rocketmq-go/docs/consume-message-activity.puml)
   
 ### cleanExpireMsg (Non-major)

when message cost too many time,we will drop this message（send message back） (for example 30 mins)



