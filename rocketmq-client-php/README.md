# rocketmq-client-php

A Php Client for Apache RocketMQ.

# dependence
* [rocketmq-client-cpp](https://github.com/apache/rocketmq-client-cpp)
* [PHP-CPP](https://github.com/CopernicaMarketingSoftware/PHP-CPP)
* php7

## INSTALL
1. install rocketmq-client-cpp
2. install php-cpp
3. download rocketmq-client-php  
```shell
    git clone https://github.com/lpflpf/rocketmq-client-php;
    cd rocketmq-client-php;
    make && make install
```
4. update php.ini file, add line `extension=rocketmq.so`;
5. try to run example in example directory.

## Usage
   to see autocompelete file.

## Example 

### Producer Example

```php
namespace RocketMQ;
$instanceName = "MessageQueue";

$producer = new Producer($instanceName);
$producer->setInstanceName($instanceName);
$producer->setNamesrvAddr("127.0.0.1:9876");
$producer->start();

for ($i = 0; $i < 10000; $i ++){
    $message = new Message("TopicTest", "*", "hello world $i");
    $sendResult = $producer->send($message);
    echo $sendResult->getSendStatus() . "\n";
}
```

### PullConsumer Example

It is a good idea to save offset in local.

```php
namespace RocketMQ;

$consumer = new PullConsumer("pullTestGroup");
$consumer->setInstanceName("testGroup");
$consumer->setTopic("TopicTest");
$consumer->setNamesrvAddr("127.0.0.1:9876");
$consumer->start();
$queues = $consumer->getQueues();

foreach($queues as $queue){
    $newMsg = true;
    $offset = 0;
    while($newMsg){
        $pullResult = $consumer->pull($queue, "*", $offset, 8);
    
        switch ($pullResult->getPullStatus()){
        case PullStatus::FOUND:
            foreach($pullResult as $key => $val){
                echo $val->getMessage()->getBody() . "\n";
            }
            $offset += count($pullResult);
            break;
        default:
            $newMsg = false;
            break;
        }
    }
}
```

### PushConsumer Example

```php
namespace RocketMQ;

$consumer = new PushConsumer("testGroup");
$consumer->setInstanceName("testGroup");
$consumer->setNamesrvAddr("127.0.0.1:9876");
$consumer->setThreadCount(10);
$consumer->setListenerType(MessageListenerType::LISTENER_ORDERLY);
$count = 0;
$consumer->setCallback(function ($msg) use (&$count){
    echo $msg->getMessage()->getBody() . "\n";
    $count ++;
});
$consumer->subscribe("TopicTest", "*");
$consumer->start();
$consumer->shutdown();

```
## TODO

1. Manual commit an offset.
2. Log handle. ( specify log file is no support by rocketmq-client-cpp. )
3. Doc.
