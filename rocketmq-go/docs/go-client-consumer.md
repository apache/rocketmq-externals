# How Go RocketMQ Client Works


1. create a new rocketMqMange instance(nameServerAddr ...)

2. create a new consumer instance(topic/tag/listener ...)(now only support cluster/concurrent)

3. consumer register to rocketMqMange

4. rocketMqMange start(StartAllScheduledTask )

## Scheduled Tasks

 * updateTopicRouteInfo
 
 update Topic Route Info by consumer subscription data (topic route info data get from name server) 
 
 put them into local memory(BrokerAddrTable/TopicPublishInfoTable/TopicSubscribeInfoTable/TopicRouteTable)

[update-topic-routeInfo-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=)

 * heartbeat:
 
 prepare heartbeat data(all consumer and producer data in this client)
 
 send it to all brokers.(broker data is from BrokerAddrTable) 
  
 (only broker know the distribution of the consumers we can rebalance)

[heartbeat-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=)

 * rebalance
 
 for each consumer's Subscription data,and rebalance
 
 (after rebalance we can know the (topic/consumer group) should consume from which broker which queue)
 
  put them into local memory(processQueueTable)
  
  enqueue pull message request (chan *model.PullRequest)
  
 [rebalance-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=)
  
 * pullMessage
 
 dequeue pull message request and pull message from broker,when get messages to consume,
 put them into consume request,consume request handler will call the listener consume the message
 
 enqueue a new pull message request and commit our consume offset to broker
 
 [pull-message-timing-diagram](http://www.plantuml.com/plantuml/proxy?src=)
 
 * cleanExpireMsg (Non-major)

when message cost too many time,we will drop this message（send message back） (for example 30 mins)



