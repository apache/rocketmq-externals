<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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