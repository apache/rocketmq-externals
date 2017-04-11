<?php
use RocketMQ\Client\Producer\DefaultMQProducer;
use RocketMQ\Common\Message\Message;
use RocketMQ\Remoting\Common\RemotingHelper;

$producer = new DefaultMQProducer("Jodie_Daily_test");
$producer->start();
$producer->setRetryTimesWhenSendAsyncFailed(0);

for ($i = 0; $i < 10000000; $i++) {

    try {
        $index = $i;
        $msg = new Message("Jodie_topic_1023",
            "TagA",
            "OrderID188",
            "Hello world" . getBytes(RemotingHelper::DEFAULT_CHARSET));
        $producer->send($msg, new class() extends SendCallback() {
            public
            function onSuccess($sendResult)
            {
                printf("%-10d OK %s %n", $index, $sendResult->getMsgId());
            }

            public
            function onException($e)
            {
                printf("%-10d Exception %s %n", $index, $e);
                $e->printStackTrace();
            }
        });
} catch (\Exception $e) {
        echo $e->getTraceAsString();
    }

}
$producer->shutdown();