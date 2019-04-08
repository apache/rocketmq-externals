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


namespace RocketMQ;

include("Message.php");

$consumer = new PullConsumer("pullTestGroup");
$consumer->setGroup("pullTestGroup");
$consumer->setInstanceName("testGroup");
$consumer->setTopic("TopicTest");
$consumer->setNamesrvAddr("127.0.0.1:9876");

$consumer->setTcpTransportConnectTimeout(100);
$consumer->setTcpTransportPullThreadNum(1);
$consumer->setTcpTransportTryLockTimeout(100);
$consumer->start();
$queues = $consumer->getQueues();

//foreach($queues as $queue){
$queue = $queues[0];
$newMsg = true;
$offset = 0;
$iter = 0;
while($newMsg){
    $pullResult = $consumer->pull($queue, "*", $offset, 16);

    switch ($pullResult->getPullStatus()){
    case PullStatus::FOUND:
        echo "pullStatus: " . $pullResult->getPullStatus() . "\n";
        //echo "count: " . $pullResult->getCount() . "\n";
        echo "nextBeginOffset: " . $pullResult->getNextBeginOffset() . "\n";
        echo "minOffset: " . $pullResult->getMinOffset() . "\n";
        echo "maxOffset: " . $pullResult->getMaxOffset() . "\n";
        echo "pullStatus: " . $pullResult->getPullStatus() . "\n";
        echo "\n";
        foreach($pullResult as $key => $msg){
            echo_msg($msg);
        }
        break;
    case PullStatus::NO_MATCHED_MSG:
    case PullStatus::OFFSET_ILLEGAL:
        $newMsg = false;
    case PullStatus::BROKER_TIMEOUT:
        $newMsg = false;
    case PullStatus::NO_NEW_MSG:
        $newMsg = false;
        break;
    default:
    }

    $offset += count($pullResult);

    $iter ++;
    if ($iter > 16){
        break;
    }else{
        echo $iter . "\t";
        echo $offset . "\t";
        echo count($pullResult) . "\n";
    }
    //		$queue->setMessageQueueOffset($position + $pullResult->getCount());
}
//}
