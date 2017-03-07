# RocketMQ User Guide

## OPS Page
* You can change console's namesvrAddr here
* You can change the value of useVIPChannel  here (if you rocketMQ version < 3.5.8,the value of useVIPChannel should be false)

## DashBoard Page
* broker's message count (broker total message count/5 min trend)
* topic's message count（topic total message count/5 min trend）

## Cluster Page
* Cluster Detail
    * relation between cluster and broker
    * broker's master / salve node
* broker'a detail info(runtime info)
* broker's config

## Topic Page
* show all the topics,you can filter topic by search bar
* filter (Normal/retry/dead) topic 
* Add/Update Topic
    * clusterName (create on which cluster)
    * brokerName (create on which broker)
    * topicName 
    * writeQueueNums  
    * readQueueNums  
    * perm //2 for write 4 for read 6 for write and read
* STATUS look over message send status(send to which broker/which queue/how many messages) 
* ROUTER look update topic's router（this topic send to which broker，the broker's queue info）
* CONSUMER MANAGE（this topic consume by which group,how about the consume state）
* TOPIC CONFIG（check or change the topic's config）
* SEND MESSAGE（send a test message）
* Reset CONSUMER OFFSET (the consumer online or not online is different,you need check the reset result)
* DELETE （will delete the topic on all broker and namesvr）

## Consumer Page
* show all the consumers,you can filter consumer by search bar
* refresh page/refresh page per 5 seconds
* order by SubscriptionGroup/Quantity/TPS/Delay
* Add/Update Consumer
    * clusterName (create on which cluster)
    * brokerName (create on which broker)
    * groupName  (consumer group name)
    * consumeEnable (this group can't consume message if this is false)
    * consumeBroadcastEnable (can't use broadcast is this is false)
    * retryQueueNums 
    * brokerId (consume form where when broker is normal)
    * whichBrokerWhenConsumeSlowly(consume form where when broker has problem)
* CLIENT (look over online consumer's client,include subscribe info and consume mode)
* CONSUME DETAIL (look over this consumer's consume detail,broker offset and the consumer offset,queue consumed by which client)
* CONFIG （check or change the consumer's config）
* DELETE (delete the consumer group on selected group)

## Producer Page
* Query online producer client by topic and group
    * show client's server / version
    
## Message Page
* Query By Topic And Time
    *Only Return 2000 Messages，the message more than 2000 will be hide
* Query By Topic And Key
    * Only Return 64 Messages
* Query By Topic And MessageId
* look over this message's detail info.you can see the message's consume state(each group has one line),show the exception message if has exception.
you can send this message to the group you selected