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


function echo_msg($msg_ext){
    $msg = $msg_ext->getMessage();
    $arr = array(
        "msgId" => $msg_ext->getMsgId(),
        "topic" => $msg->getTopic(),
        "tags" => $msg->getTags(),
        "storeHostString" => $msg_ext->getStoreHostString(),
        "bornTimestamp" => $msg_ext->getBornTimestamp(),
        "queueId" => $msg_ext->getQueue(),
        "getBornTimestamp" => $msg_ext->getBornTimestamp(),
        "offsetMsgId" => $msg_ext->getOffsetMsgId(),
        "bodyCRC" => $msg_ext->getBodyCRC(),
        "queueOffset" => $msg_ext->getQueueOffset(),
        "commitLogOffset" => $msg_ext->getCommitLogOffset(),
        "storeSize" => $msg_ext->getStoreSize(),
        "reconsumeTimes" => $msg_ext->getReconsumeTimes(),
        "preparedTransactionOffset" => $msg_ext->getPreparedTransactionOffset(),
        "keys" => $msg->getKeys(),
        "delayTimeLevel" => $msg->getDelayTimeLevel(),
        "isWaitStoreMsgOK" => $msg->isWaitStoreMsgOK() ? "true" : "false",
        "flag" => $msg->getFlag(),
        "sysFlag" => $msg->getSysFlag(),
        "body" => substr($msg->getBody(),0,40),

    );
    
    foreach($arr as $key => $val){
        printf("|%-30s|%-40s|\n", $key, $val);
    }
    echo "-------------------------------------------------------------------------\n";
}

