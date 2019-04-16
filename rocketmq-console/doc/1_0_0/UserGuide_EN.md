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


## Access Console with HTTPS
* SpringBoot itself has provided the SSL configuration. You can use the project test Keystore:resources/rmqcngkeystore.jks. The store is generated with the following unix keytool commands:
```
#Generate Keystore and add alias rmqcngKey
keytool -genkeypair -alias rmqcngKey  -keyalg RSA -validity 3650 -keystore rmqcngkeystore.jks 
#View keystore content
keytool -list -v -keystore rmqcngkeystore.jks 
#Transfer type as official 
keytool -importkeystore -srckeystore rmqcngkeystore.jks -destkeystore rmqcngkeystore.jks -deststoretype pkcs12 
```

* Uncomment the following SSL properties in resources/application.properties. restart Console then access with HTTPS.

```
#Set https port
server.port=8443

### SSL setting
server.ssl.key-store=classpath:rmqcngkeystore.jks
server.ssl.key-store-password=rocketmq
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=rmqcngkey
```

## Login/Logout on Console
Access Console with username and password and logout to leave the console。To stage the function on, we need the steps below:

* 1.Turn on the property in resources/application.properties.
```$xslt
# open the login func
rocketmq.config.loginRequired=true

# Directory of ashboard & login user configure file 
rocketmq.config.dataPath=/tmp/rocketmq-console/data
```
* 2.Make sure the directory defined in property ${rocketmq.config.dataPath} exists and the file "users.properties" is created under it. 
The console system will use the resources/users.properties by default if a customized file is not found。

The format in the content of users.properties:
```$xslt
# This file supports hot change, any change will be auto-reloaded without Console restarting.
# Format: a user per line, username=password[,N] #N is optional, 0 (Normal User); 1 (Admin)

# Define Admin
admin=admin,1

# Define Normal users
user1=user1
user2=user2
```
* 3. Restart Console Application after above configuration setting well.  