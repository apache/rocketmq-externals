#RocketMQ-Console-Ng[![Build Status](https://travis-ci.org/rocketmq/rocketmq-console-ng.svg?branch=master)](https://travis-ci.org/rocketmq/rocketmq-console-ng) [![Coverage Status](https://coveralls.io/repos/github/rocketmq/rocketmq-console-ng/badge.svg?branch=master)](https://coveralls.io/github/rocketmq/rocketmq-console-ng?branch=master)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
#How To Install

## With Docker

* get docker image

```
mvn package docker:build
```

or

```
docker pull styletang/rocketmq-console
```
* run it (change namesvrAddr and port yourself)

```
docker run -e "JAVA_OPTS=-Drocketmq.namesrv.addr=127.0.0.1:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8080:8080 -t styletang/rocketmq-console
```

## Without Docker
require java 1.7
```
mvn spring-boot:run
```
or
```
mvn clean package -Dmaven.test.skip=true
java -jar target/rocketmq-console-ng-1.0.0-SNAPSHOT.jar
```

###tips
* if you download package slow,you can change maven's mirror(maven's settings.xml)
  
  ```
  <mirrors>
      <mirror>
            <id>alimaven</id>
            <name>aliyun maven</name>
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
            <mirrorOf>central</mirrorOf>        
      </mirror>
  </mirrors>
  ```
  
* if you use the rocketmq < 3.5.8,please add -Dcom.rocketmq.sendMessageWithVIPChannel=false when you start rocketmq-console
* change the rocketmq.namesrv.addr in resource/application.properties.(or you can change it in ops page)

#UserGuide
[English]()
[中文]()

#Communicate With Us
* QQ Group:535273860
* You can communicate with us use QQ.(or send us issue / pull request)
* You can join us and make a contribute for rocketmq-console-ng.
