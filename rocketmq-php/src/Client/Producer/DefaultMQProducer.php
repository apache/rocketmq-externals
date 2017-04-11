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
namespace RocketMQ\Client\Producer;

use RocketMQ\Client\Common\ClientErrorCode;
use RocketMQ\Client\Exception\MQClientException;
use RocketMQ\Client\Latency\MQFaultStrategy;
use RocketMQ\Common\CommunicationMode;
use RocketMQ\Common\Message\Message;
use RocketMQ\Common\System;

class DefaultMQProducer
{

    public $mqFaultStrategy;
    public function __construct()
    {
        $this->mqfaultStrategy = new MQFaultStrategy();

    }
    public function start()
    {

    }

    public function shutdown()
    {

    }


    public function updateFaultItem($brokerName, $currentLatency, $isolation) 
    {

    }
    /**
     * @param Message $msg
     */
    public function send($msg, $communicationMode = CommunicationMode::SYNC, $sendCallback = null, $timeout = null)
    {
        $this->makeSureStateOK();
        Validators::checkMessage($msg, $this->defaultMQProducer);

        $invokeID = random::nextLong();
        $beginTimestampFirst = System::currentTimeMillis(); //System.currentTimeMillis()
        $beginTimestampPrev = $beginTimestampFirst;
        $endTimestamp = $beginTimestampFirst;
        $topicPublishInfo = $this->tryToFindTopicPublishInfo($msg->getTopic());
        if ($topicPublishInfo != null && $topicPublishInfo->ok()) {
            $mq = null;
            $exception = null;
            $sendResult = null;
            $timesTotal = $communicationMode === CommunicationMode::SYNC ? 1 + $this->defaultMQProducer->getRetryTimesWhenSendFailed() : 1;
            $times = 0;
            $brokersSent = '';
            for (; $times < $timesTotal; $times++) {
                $lastBrokerName = null == $mq ? null : $mq->getBrokerName();
                $tmpmq = $this->selectOneMessageQueue($topicPublishInfo, $lastBrokerName);
                if ($tmpmq != null) {
                    $mq = $tmpmq;
                    $brokersSent[$times] = $mq->getBrokerName();
                    try {
                        $beginTimestampPrev = System::currentTimeMillis();
                        $sendResult = $this->sendKernelImpl($msg, $mq, $communicationMode, $sendCallback,
                            $topicPublishInfo, $timeout);
                        $endTimestamp = System::currentTimeMillis();
                        $this->updateFaultItem($mq->getBrokerName(), $endTimestamp - $beginTimestampPrev, false);
                        switch ($communicationMode) {
                            case CommunicationMode::ASYNC:
                                return null;
                            case CommunicationMode::ONEWAY:
                                return null;
                            case CommunicationMode::SYNC:
                                if ($sendResult->getSendStatus() != SendStatus::SEND_OK) {
                                    $this->defaultMQProducer->isRetryAnotherBrokerWhenNotStoreOK();
                                }

                                return $sendResult;
                            default:
                                break;
                        }
                    } catch (\Exception $e) {
                        $endTimestamp = System::currentTimeMillis();
                        $this->updateFaultItem($mq->getBrokerName(), $endTimestamp - $beginTimestampPrev, true);
                        $this->log->warn(sprintf("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s",
                            invokeID, endTimestamp - beginTimestampPrev, mq), $e);
                        $this->log->warn($msg->toString());


                        if ($sendResult != null) {
                            return $sendResult;
                        }

                        $info = sprintf("Send [%d] times, still failed, cost [%d]ms, Topic: %s, BrokersSent: %s",
                            $times,
                            System::currentTimeMillis() - $beginTimestampFirst,
                            $msg->getTopic(),
                            var_export($brokersSent, 1));

                        $info += FAQUrl::suggestTodo(FAQUrl::SEND_MSG_FAILED);

                        $mqClientException = new MQClientException($info, $exception);

                        throw $mqClientException;
                    }

                    $nsList = $this->getmQClientFactory()->getMQClientAPIImpl()->getNameServerAddressList();
                    if (null == $nsList || $nsList->isEmpty()) {
                        throw (new MQClientException(
                            "No name server address, please set it->" + FAQUrl::suggestTodo(FAQUrl::NAME_SERVER_ADDR_NOT_EXIST_URL),
                            null))->setResponseCode(ClientErrorCode::NO_NAME_SERVER_EXCEPTION);
                    }

                    throw (new MQClientException("No route info of this topic, " + $msg->getTopic() + FAQUrl::suggestTodo(FAQUrl::NO_TOPIC_ROUTE_INFO),
                        null))->setResponseCode(ClientErrorCode::NOT_FOUND_TOPIC_EXCEPTION);
                }
            }
        }
    }
}