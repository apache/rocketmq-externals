#TODO TODO TODO
We have many things to discuss

we will deploy the first rocketmq-console-ng  use rocketmq-tools 3.5.8(or 4.0.0),base on [rocket-console](https://github.com/didapinchegit/rocket-console)，thanks didapinche.com


#### some problems
* 0. which framework shall we use (spring-boot + bootstrap + angularjs?)(todo discuss)

#### something to improve
* 1. international
* 2. compress fe'resource
* 3. navigation bar can improve
* 4. write operation need confirm,action show the detail result
* 6. layout/UI should improve


#### something to fix
* query Message by topic and time is not accurate， will lost some message 
* consumer can consume the message when topic has been deleted
* can't show producerList,we can only query a online producer use topic and groupName,not easy to use.
* resetOffset should be improve,online consumer can return the reset result but offline's can't
* we can't set clusterName when create topic or consumer 
* when create a new consumer,if not be consumed,can't be found in consumerList
* Config （namesvr/broker）

#### something to add
* 1. dashboard 




# Roadmap

## dashboard
- [ ] DashboardController
    - [ ] rocketmq topic tps 5m line chart
    - [ ] rocketmq topic top10 table
    - [ ] broker load 5m line chart
    - [ ] broker load top10 table
    - [ ] topic exception table

## Cluster
- [ ] ClusterController
    - [ ] Cluster OverView
    - [ ] Broker Status
    - [ ] Broker Config

## Topic
- [ ] TopicController
    - [ ] TopicList
    - [ ] Topic Status
    - [ ] Topic Router
    - [ ] View Topic Config
    - [ ] Topci Add / Update
    - [ ] Send A Test Topic
    - [ ] Reset ConsumerGroup's Offset Under This Topic
    - [ ] Delete This Topic

## Producer
- [ ] ProducerController
    - [ ] ProducerList
    - [ ] Producer Client Info


## Consumer
- [ ] ConsumerController
    - [ ] ConsumerList
    - [ ] Consumer Client Info
    - [ ] Topic Consume Status Under This Consumer Group
    - [ ] View Consumer Config
    - [ ] Consumer Add / Update
    - [ ] Delete This Consumer

## Message
- [ ] MessageController
    - [ ] Query By Topic And Time
    - [ ] Query By Topic And Key
    - [ ] Query By MessageId(OffsetMessageId)
    - [ ] A Nice Message Detail View
    - [ ] Message Consume Status
    - [ ] Resend Message To A Consume Group

## Config (todo disscuss is it necessary)
- [ ] ConfigController
    - [ ] BrokerConfig
    - [ ] NamesvrKvConfig


