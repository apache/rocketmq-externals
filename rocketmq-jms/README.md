# RocketMQ-JMS   [![Build Status](https://travis-ci.org/rocketmq/rocketmq-jms.svg?branch=master)](https://travis-ci.org/rocketmq/rocketmq-jms) [![Coverage Status](https://coveralls.io/repos/github/rocketmq/rocketmq-jms/badge.svg?branch=master)](https://coveralls.io/github/rocketmq/rocketmq-jms?branch=master)


## Introduction
RocketMQ-JMS is an implement of JMS specification,taking Apache RocketMQ as broker.
Now we are on the way of supporting JMS 1.1 and JMS2.0 is our final target.   

Now RocketMQ-JMS will release the first version soon, and new features will be developed on the branch "v1.1".
Please visit the [issue board](https://github.com/rocketmq/rocketmq-jms/issues) to see features in next version. 


## Building

  > cd rocketmq-jms  
  > mvn clean install  
  
  **run unit test:**  
  > mvn test    
  
  **run integration test:**  
  > mvn verify
  
  **see jacoco code coverage report**
  > open core/target/site/jacoco/index.html  
  > open core/target/site/jacoco-it/index.html  
  > open spring/target/site/jacoco-it/index.html 
  
  
## Guidelines

 Please see [Coding Guidelines Introduction](http://rocketmq.apache.org/docs/code-guidelines/)
