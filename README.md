# RocketMQ-JMS  


## Introduction
RocketMQ-JMS is an implement of JMS specification,taking Apache RocketMQ as broker.
Now we are on the way of supporting JMS 1.1 and JMS2.0 is our final target.   

Java 7 should be used for building as JMS specification over 2.0 only support at least Java 7.

Now RocketMQ-JMS will release the first version soon, and new features will be developed on the branch "v1.1".
Please visit the [issue board](https://github.com/rocketmq/rocketmq-jms/issues) to see features in next version. 


## Building
````
  cd rocketmq-jms
  mvn clean install  
  ````  
  **run unit test:**  mvn test    
  
  **run integration test:**  mvn verify
  
## Guidelines

 Please see [Coding Guidelines Introduction](http://rocketmq.apache.org/docs/code-guidelines/)
