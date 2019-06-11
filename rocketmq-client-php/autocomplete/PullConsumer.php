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

/**
 * Pull method consumer.
 *
 */
class PullConsumer{
    public function __construct(string $groupName){
    }

    public function setInstanceName($instanceName){
    }

    public function setNamesrvAddr($address){
    }

    public function getNamesrvAddr(){
    }

    public function setNamesrvDomain($nameserver){
    }

    public function getNamesrvDomain(){
    }

    public function setTopic($topic){
    }

    public function start(){
    }

    public function getQueues(){
    }

    public function setGroup(string $group){
    }

    public function pull(MessageQueue $messageQueue, string $subExpression, int $offset, int $maxNums){
    }

    public function pullBlockIfNotFound(MessageQueue $messageQueue, string $subExpression, int $offset, int $maxNums){
    }

    public function getSessionCredentials(){
    }

    public function setSessionCredentials(string $accessKey, string $secretKey, string $authChannel){
    }

    public function updateConsumeOffset(MessageQueue $messageQueue, int $offset){
    }

    public function removeConsumeOffset(MessageQueue $messageQueue){
    }

    public function fetchConsumeOffset(MessageQueue $messageQueue, string $fromStore){
    }

    public function setMessageModel($model){
    }

    public function getMessageModel($model){
    }

    public function setTcpTransportPullThreadNum(int $num){
    }

    public function getTcpTransportPullThreadNum(){
    }

    public function setTcpTransportConnectTimeout(int $timeout){
    }

    public function getTcpTransportConnectTimeout(){
    }

    public function setTcpTransportTryLockTimeout(int $timeout){
    }

    public function getTcpTransportTryLockTimeout(){
    }

    public function setUnitName(string $unitName){
    }

    public function getUnitName(){
    }

    public function getConsumeFromWhere(){
    }

    public function setConsumeFromWhere(int $consumeFromWhere){
    }

    public function setLogLevel(int $inputLevel){
    }

    public function getLogLevel(){
    }

    public function setLogFileSizeAndNum($fileNum, $perFileSize){
    }
}
