# RocketMQ-JMS  


## Introduction
RocketMQ-JMS is an implement of JMS specification,taking Apache RocketMQ as broker.
Now we are on the way of supporting JMS 1.1 and JMS2.0 is our final target.   

Java 7 should be used for building as JMS specification over 2.0 only support at least Java 7.

## Support features
 
 - Support basic features that producing and consuming messages smoothly.
 - Support mandatory JMS headers, built-in properties and user properties.
 - Support both point-to-point and publish/subscribe models.
 - Support both synchronous and asynchronous consume model.
 - Support unshared non-durable, unshared durable, shared non-durable, shared durable subscription.
 - Support features such as message order, AUTO_ACKNOWLEDGE.
 - Follow single-thread restriction of session.
 - Support Spring JMS 4 partially.
  
## Building
````
  cd rocketmq-jms
  mvn clean install  
  ````  
  **run unit test:**  mvn test    
  
  **run integration test:**  mvn verify
  
## Guidelines

 Please see [Coding Guidelines Introduction](http://rocketmq.apache.org/docs/code-guidelines/)