<?php
use RocketMQ\Client\Producer\DefaultMQProducer;
use RocketMQ\Common\Message\Message;

$producer = new DefaultMQProducer("ProducerGroupName");
$producer->start();

for ($i = 0; $i < 10000000; $i++) {
    try {
        $msg = new Message("TopicTest", 
        "TagA",
        "OrderID188",
        "Hello world");
        $sendResult = $producer->send($msg);
        echo $sendResult;
    } catch (\Exception $e) {
        echo $e->getMessage() . PHP_EOL . $e->getTraceAsString();
    }
}
$producer->shutdown();