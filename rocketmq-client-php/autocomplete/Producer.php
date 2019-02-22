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


class Producer {
    public function __construct(string $groupName){
    }

    public function getMQClientId(){
    }

    public function getInstanceName(){
    }

    public function setInstanceName(string $groupName){
    }

    public function getNamesrvAddr(){
    }

    public function setNamesrvAddr(string $nameserver){
    }

    public function setNamesrvDomain($nameserver){
    }

    public function getGroupName(){
    }

    public function setGroupName(string $groupName){
    }

    public function send(Message $message){
    }

    public function getSessionCredentials(){
    }

    public function setSessionCredentials(string $accessKey, string $secretKey, string $autChannel){
    }

    public function getTopicMessageQueueInfo(string $topic){
    }

    public function start(){
    }

    public function setRetryTimes(int $retryTimes){
    }

    public function getRetryTimes(){
    }

    public function getSendMsgTimeout(){
    }

    public function setSendMsgTimeout($sendMsgTimeout){
    }

    public function  getCompressMsgBodyOverHowmuch(){

    }

    public function setCompressMsgBodyOverHowmuch($compressMsgBodyOverHowmuch){
    }

    //   level = [-1, 9]
    public function getCompressLevel(){
    }

    //   level = [-1, 9]
    public function setCompressLevel($compressLevel){
    }

    public function getMaxMessageSize(){
    }

    public function setMaxMessageSize($messageSize){
    }

    // default = cpu core.
    public function setTcpTransportPullThreadNum(int $num){
    }

    public function getTcpTransportPullThreadNum(){
    }

    // default = 3000 ms
    public function setTcpTransportConnectTimeout(int $timeout){
    }

    public function getTcpTransportConnectTimeout(){
    }

    // default 3000ms 
    public function setTcpTransportTryLockTimeout(int $timeout){
    }

    public function getTcpTransportTryLockTimeout(){
    }

    // default unitName = ""
    public function setUnitName(string $unitName){
    }

    public function getUnitName(){
    }

    public function setLogLevel(int $inputLevel){
    }

    public function getLogLevel(){
    }

    public function setLogFileSizeAndNum($fileNum, $perFileSize){
    }
}

