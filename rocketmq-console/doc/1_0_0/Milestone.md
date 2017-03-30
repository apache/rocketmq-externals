#Deploy Plan

we will deploy the first rocketmq-console-ng use rocketmq-tools 3.5.8(or 4.0.0),base on [rocket-console](https://github.com/didapinchegit/rocket-console)，thanks didapinche.com

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
- [x] clean code (checkStyle codeStyle to be done) -- StyleTang
- [x] international -- Deploy by [tcrow](https://github.com/tcrow)
- [x] layout/UI  -- Deploy by [tcrow](https://github.com/tcrow)
	- [x] compress fe'resource 
	- [x] navigation bar can improve
	- [x] write operation need confirm,action show the detail result || already have
	- [x] layout/UI should improve
- [x] change to spring-boot  -- Deploy by syzjava
- [x] change to bootstrap angularjs   -- Deploy by [tcrow](https://github.com/tcrow)
- [x] improve search message --StyleTang

## Fix
- [x] query Message by topic and time is not accurate， will lost some message  -- StyleTang (need test)
- [x] consumer can consume the message when topic has been deleted // offset be clear.if have problem,reopen it.
- [ ] can't show producerList,we can only query a online producer use topic and groupName,not easy to use. [need this issues](https://issues.apache.org/jira/browse/ROCKETMQ-49)(next milestone)
- [ ] resetOffset should be improve,online consumer can return the reset result but offline's can't //this version(3.5.8) may be can't fix (next milestone)
- [x] we can't set clusterName when create topic or consumer  -- StyleTang
- [x] when create a new consumer,if not be consumed,can't be found in consumerList //it Fixed,But this page is too slow,need improve --StyleTang
- [x] message view page,resend message (version >=3.5.8) have bug   -- StyleTang

## Add
- [ ] DashboardController      -- Deploy by [tcrow](https://github.com/tcrow)
    - [x] rocketmq topic tps 5m line chart
    - [x] rocketmq topic top10 table
    - [x] broker load 5m line chart
    - [x] broker load top10 table
    - [ ] topic exception table(next milestone)

## Already Have (Deploy by StyleTang) But Can Improve 
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
    
    