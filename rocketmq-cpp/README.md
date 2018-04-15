## Introduction
* RocketMQ client for linux/windows cpp is the kernel implementation of aliyun MQ cpp SDK(https://help.aliyun.com/document_detail/29555.html?spm=5176.doc29532.6.593.yidJeD), and had gone through 3 years time-tested in Alibaba Group，and had been used widly by many services, such as IM service, Navigation, advertisement tool, on-line shopping service and so on;
## Characteristics
* 1>. disaster recovery ability
    - Based on nameServer snapshot and network disaster recovery strategy, no real-time impact on publish/subscribe when anyone of broker or nameSrv was broken
* 2>. low latency
    - publish latency < 2ms, subscribe latency < 10ms
* 3>. High publish/subsricbe TPS
    - For 16 message queues and stand-alone cpp client, publish TPS > 3W, subsricbe TPS > 15W
* 4>. support all rocketmq features
    - Such as broadcast/cluster model, concurrency/orderly publish/subscribe, timed/delay msg, consumer status query and so on. 
* 5>. support across platform
    - all features are supported on both windows and linux system.
* 6>. support Authentication on AliYun by ak/sk
    - Access AliYun Authentication by calling one interface: MQClient::setSessionCredentials
## User manaual: doc/rocketmq-cpp_manaual_zh.docx
## How to build
### Linux platform
#### 1. Dependency Installation
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
	 - Download [boost 1.56.0](http://www.boost.org/users/history/version_1_56_0.html)
	 - Build and install boost
	   - cd path/to/boost_1_56_0
	   - config boost：./bootstrap.sh
	   - build boost:     
	       - build static boost lib: ./b2 link=static runtime-link=static
	       - build dynamic boost lib: ./b2 link=shared runtime-link=shared
	   -  install boost: ./b2 install
	   
#### 2. Make and Install
* Default install path:
    - header files: /usr/local/include
    - lib: /usr/local/lib
* Make and install using cmake
    - cmake will auto find_package, if failes, change BOOST_INCLUDEDIR/LIBEVENT_INCLUDE_DIR/JSONCPP_INCLUDE_DIR in CMakeList.txt, according to its real install path
    - make
    - make install
	
#### 3. Check verion
- strings librocketmq.so |grep VERSION
	
### Windows platform:
#### 1. Dependency Installation
* Install libevent 2.0.22 dependency
    - dowload libevent 2.0.22
        - https://github.com/libevent/libevent/releases/download/release-2.0.22-stable/libevent-2.0.22-stable.tar.gz
    - build and install libevent
        - extract libevent to C:/libevent 
        - open VirtualStudio command line tools, go to dir: C:/libevent
        - execute cmd: nmake /f Makefile.nmake
        - cp libevent.lib, libevent_extras.lib and libevent_core.lib to C:/libevent/lib
* install JsonCPP 0.10.6 dependency
    - download jsoncpp 0.10.6
        - https://github.com/open-source-parsers/jsoncpp/archive/0.10.6.zip
    - build and install jsoncpp
        - extract jsoncpp to C:/jsoncpp
        - download cmake windows tool(https://cmake.org/files/v3.9/cmake-3.9.3-win64-x64.zip) and extract
        - run cmake-gui.exe, choose your source code dir and build dir, then click generate which will let you choose VirtualStudio version
        - open project by VirtualStudio, and build jsoncpp, and jsoncpp.lib will be got
* install boost 1.56.0 dependency
    - dowload boost 1.56.0
        - http://www.boost.org/users/history/version_1_56_0.html
    - build and install boost 1.56.0
        - according to following discription: http://www.boost.org/doc/libs/1_56_0/more/getting_started/windows.html
        - following build options are needed to be set when run bjam.exe: msvc architecture=x86 address-model=64 link=static runtime-link=static stage
        - all lib will be generated except boost_zlib:
            - download zlib source: http://gnuwin32.sourceforge.net/downlinks/zlib-src-zip.php and extract to directory C:/zlib
            - run cmd:bjam.exe msvc architecture=x86 address-model=64 link=static runtime-link=static --with-iostreams -s ZLIB_SOURCE=C:\zlib\src\zlib\1.2.3\zlib-1.2.3 stage

#### 2. Make and Install
* generate project solution by cmake automatically
    - download cmake windows tool(https://cmake.org/files/v3.9/cmake-3.9.3-win64-x64.zip) and extract
    - run cmake-gui.exe, choose your source code dir and build dir, then click generate which will let you choose VirtualStudio version
    - if generate project solution fails, change BOOST_INCLUDEDIR/LIBEVENT_INCLUDE_DIR/JSONCPP_INCLUDE_DIR in CMakeList.txt, according to its real install path
* open&build&run project by VirtualStudio

### log path:$HOME/logs/rocketmq-cpp

## Description for Important Parameters  
- -n	: nameserver addr, if not set -n and -i ,no nameSrv will be got
- -i	: nameserver domain name,  if not set -n and -i ,no nameSrv will be got
- Notice: oper should only set one option from -n and -i, 
- -g	: groupName, contains producer groupName and consumer groupName
- -t	: msg topic
- -m	: message count(default value:1)
- -c 	: msg content(default value: only test)
- -b	: consume model(default value: CLUSTER)
- -a	: set sync push(default value: async)
- -r	: setup retry times(default value:5 times)
- -u	: select active broker to send msg(default value: false)
- -d	: use AutoDeleteSendcallback by cpp client(defalut value: false)
- -T	: thread count of send msg or consume msg(defalut value: system cpu core number)
- -v 	: print more details information

- Example:
  - sync producer: ./SyncProducer -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr
  - async producer: ./AsyncProducer  -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr 
  - send delay msg: ./SendDelayMsg  -g producerGroup -t topic -c msgContent -n nameServerAddr
  - sync pushConsumer: ./PushConsumer  -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr -s sync
  - async pushConsumer: ./AsyncPushConsumer  -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr
  - orderly sync pushConsumer:  ./OrderlyPushConsumer -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr -s sync
  - orderly async pushConsumer: ./OrderlyPushConsumer -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr
  - sync pullConsumer:./PullConsumer  -g producerGroup -t topic -c msgContent -m msgCount -n nameServerAddr 

## Best practice
- producer must invoke following interface:
  - DefaultMQProducer g_producer("please_rename_unique_group_name");
  - g_producer.start();
  - g_producer.send(...);
  - g_producer.shutdown();

- pullconsumer must invoke following interface:
  - DefaultMQPullConsumer     g_consumer("please_rename_unique_group_name");
  - g_consumer.start();
  - g_consumer.fetchSubscribeMessageQueues(..., ...);
  - g_consumer.pull(...)
  - g_consumer.shutdown();

- pushconsumer must invoke following interface:
  - DefaultMQPushConsumer g_consumer("please_rename_unique_group_name_1");
  - g_consumer.subscribe("test_topic", "*");
  - g_consumer.registerMessageListener(listener);
  - g_consumer.start();
  - g_consumer.shutdown();
  
