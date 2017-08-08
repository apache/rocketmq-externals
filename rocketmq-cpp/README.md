=================meaning of each parameter===================
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

==================================Notice=============================================
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

=======================================build and install=============================================
- linux platform:
  - intall boost(stored in rpm dir) to path:A
  - modify BOOST_INCLUDE of project/Makefile to A/include, modify BOOST_LIB of example/Makefile to A/lib 
  - make
  - make install
- Windows platform:
  - will be supported later
  
- default install path:
  - header files: /usr/local/include
  - lib: /usr/local/lib

- check verion:
  - strings librocketmq.so |grep VERSION

- log path:$HOME/logs/metaq-client4cpp

- Before Run:
  - export LD_LIBRARY_PATH=/xxx/rocketmq-client4cpp/bin/:$LD_LIBRARY_PATH;LD_LIBRARY_PATH=/A/lib:$LD_LIBRARY_PATH'
