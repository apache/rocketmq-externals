# rocketmq-jms

## Introduction
RocketMQ-JMS is an implement of JMS specification,taking Apache RocketMQ as broker.
Now newest jms specification supported is JMS2.0. 
Although JMS2.1 early draft had been out in Oct 2015, it has been withdrawn from J2EE 8.   

Java 7 should be used for building as JMS specification over 2.0 only support at least Java 7.

Now RocketMQ-JMS has no release version and is still under developing. Hopefully you can contribute a bit for it :-)

## RoadMap of first release(1.0)
  #### milestone-1
  1. Refactor and perfect Message Model.
  2. Refactor and perfect Message Domain(including but not limited p2p and pub/sub excluding temp/browser/requestor). 
  3. Refactor and perfect Connection(including but not limited setup/start/stop/metadata/exception/close).
  4. Refactor and perfect Session(including but not limited messageOrder/ack/serial/threadRestriction).
  5. Refactor and perfect Producer(including but not limited synchronous/asynchronous).
  6. Refactor and perfect Consumer()including but not limited synchronous,asynchronous).

  #### milestone-2
  1. Implement simplified api added in JMS2.0. 
  2. Complete temp/browser/requestor of Message Domain.
  3. Implement selector which filter message sending to consumer.
  4. Think of Local Transaction(distinguished with Distributed Transaction) and implement it if possible.
  5. Other missing feature/constrain.
  
## Feature must not include in newest RocketMQ-JMS release 
  1. Distributed Transaction.
  2. JMS application server facilities.
  3. Message could expire at any time in the future.
  

## Building
````
  cd RocketMQ-JMS   
  mvn clean install  
  ````  
  **run unit test:**  mvn test    
  
  **run integration test:**  mvn verify
  
## TODO
* Add guidelines for new contributors
* Add travis for Continuous Integration
