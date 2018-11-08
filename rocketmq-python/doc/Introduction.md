----------
## How to build

#### 1. Python Version
* Support python 2.7.X


#### 2. Dependency Installation
* Install libevent 2.0.22 dependency
    - Download [libevent 2.0.22](https://github.com/libevent/libevent/releases/download/release-2.0.22-stable/libevent-2.0.22-stable.tar.gz)
    - Build and install libevent
	   - ./configure
	   - make
	   - make install 
* Install JsonCPP 0.10.6 dependency
    - Download [jsoncpp 0.10.6](https://github.com/open-source-parsers/jsoncpp/archive/0.10.6.zip)
    - Build and install jsoncpp
  	     - cmake .
  	     - make
  	     - make install
* Install boost 1.56.0 dependency
	 - Download [boost 1.53.0](http://www.boost.org/users/history/version_1_53_0.html)
	 - Build and install boost
	   - cd path/to/boost_1_53_0
	   - config boost：./bootstrap.sh
	   - build boost:     
	       - build static boost lib: ./b2 link=static runtime-link=static
	       - build dynamic boost lib: ./b2 link=shared runtime-link=shared
	   -  install boost: ./b2 install
* Install librocketmq dependency
    - Download [librocketmq](https://github.com/apache/rocketmq-client-cpp)
    - Build and install librocketmq
	   - make
	   - make install 	
* Install Boost-python 1.53.0 dependency
    - install boost-python
      
#### 3. Make and Install
* Default install path:
    - header files: /usr/local/include/
    - lib: /usr/local/lib
* Make and install using make
    - make
    - make install
	
#### 4. Check verion
- strings librocketmqclientpython.so |grep PYTHON_CLIENT_VERSION

----------
## Best practice

- create message by following interface:
  - msg = CreateMessage("your_topic.")
  - SetMessageBody(msg, "this_message_body.")
  - SetMessageKeys(msg, "this_message_keys.")
  - SetMessageTags(msg, "this_message_tag.")
  
- producer must invoke following interface:
  - producer =CreateProducer("please_rename_unique_group_name");
  - SetProducerNameServerAddress(producer,"please_rename_unique_name_server")
  - StartProducer(producer)
  - SendMessageSync(producer,msg)
  - ShutdownProducer(producer)
  - DestroyProducer(producer)

- how to consumer meaasges
  - def consumerMessage(msg):
  - topic = GetMessageTopic(msg)
  - body = GetMessageBody(msg)
  - tags = GetMessageTags(msg)
  - msgid = GetMessageId(msg)
  - handle message
  - return 0

- pushconsumer must invoke following interface:
  - consumer =CreatePushConsumer("please_rename_unique_group_name_1");
  - SetPushConsumerNameServerAddress(consumer,"please_rename_unique_name_server")
  - Subscribe(consumer, "your_topic", "*")
  - RegisterMessageCallback(consumer,consumerMessage)
  - StartPushConsumer(consumer)
  - ShutdownPushConsumer(consumer)
  - DestroyPushConsumer(consumer)

----------
## Demo
- sync producer
  - python testProducer.py
- push consumer
  - python testConsumer.py