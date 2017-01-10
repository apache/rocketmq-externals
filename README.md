#Deploy Plan

we will deploy the first rocketmq-console-ng  use rocketmq-tools 3.5.8(or 4.0.0),base on [rocket-console](https://github.com/didapinchegit/rocket-console)，thanks didapinche.com

## Framework
* 0. we use spring-boot + bootstrap + angularjs

## something to improve
* 0. clean code (checkStyle codeStyle to be done)
* 1. international
* 2. compress fe'resource
* 3. navigation bar can improve
* 4. write operation need confirm,action show the detail result
* 5. layout/UI should improve
* 6. change to spring-boot


## something to fix
* query Message by topic and time is not accurate， will lost some message 
* consumer can consume the message when topic has been deleted
* can't show producerList,we can only query a online producer use topic and groupName,not easy to use.
* resetOffset should be improve,online consumer can return the reset result but offline's can't
* we can't set clusterName when create topic or consumer 
* when create a new consumer,if not be consumed,can't be found in consumerList


## something to add
* 1. dashboard 




# Roadmap



## Improve
- [ ] clean code (checkStyle codeStyle to be done) -- StyleTang
- [ ] international
- [ ] layout/UI
	- [ ] compress fe'resource
	- [ ] navigation bar can improve
	- [ ] write operation need confirm,action show the detail result
	- [ ] layout/UI should improve
- [ ] change to spring-boot bootstrap angularjs
- [ ] improve search message
- [ ] refactoring old code 


## Fix
- [ ] query Message by topic and time is not accurate， will lost some message 
- [ ] consumer can consume the message when topic has been deleted
- [ ] can't show producerList,we can only query a online producer use topic and groupName,not easy to use.
- [ ] resetOffset should be improve,online consumer can return the reset result but offline's can't
- [ ] we can't set clusterName when create topic or consumer 
- [ ] when create a new consumer,if not be consumed,can't be found in consumerList

## Add
- [ ] DashboardController      -- Deploy by [tcrow](https://github.com/tcrow)
    - [ ] rocketmq topic tps 5m line chart
    - [ ] rocketmq topic top10 table
    - [ ] broker load 5m line chart
    - [ ] broker load top10 table
    - [ ] topic exception table

## Already Have But Can Improve
### Cluster
- [x] ClusterController
    - [x] Cluster OverView
    - [x] Broker Status
    - [x] Broker Config

### Topic
- [x] TopicController
    - [x] TopicList
    - [x] Topic Status
    - [x] Topic Router
    - [x] View Topic Config
    - [x] Topci Add / Update
    - [X] Send A Test Topic
    - [x] Reset ConsumerGroup's Offset Under This Topic
    - [x] Delete This Topic

### Producer
- [x] ProducerController
    - [x] ProducerList
    - [x] Producer Client Info


### Consumer
- [x] ConsumerController
    - [x] ConsumerList
    - [x] Consumer Client Info
    - [x] Topic Consume Status Under This Consumer Group
    - [x] View Consumer Config
    - [x] Consumer Add / Update
    - [x] Delete This Consumer

### Message
- [x] MessageController
    - [x] Query By Topic And Time
    - [x] Query By Topic And Key
    - [x] Query By MessageId(OffsetMessageId)
    - [x] A Nice Message Detail View
    - [x] Message Consume Status
    - [x] Resend Message To A Consume Group
