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

class Message {
	public function __construct(string $topic, string $tags, string $body = "", string $keys){
	}
	public function setProperty(string $name, string $value){
	}
	public function getProperty(){
	}
	public function getTopic(){
	}
	public function setTopic(string $topic){
	}
	public function setTags(string $tags){
	}
	public function getKeys(){
	} 
	public function setKeys(string $keys){
	}
	public function getDelayTimeLevel(){
	}
	public function setDelayTimeLevel(int $delayTimeLevel){
	}
	public function isWaitStoreMsgOK(){
	}
	public function setWaitStoreMsgOK(bool $waitStoreMsgOK){
	}
	public function getFlag(){
	}
	public function setFlag(int $flag){
	}
	public function getSysFlag(){
	}
	public function setSysFlag(int $sysFlag){
	}
	public function getBody(){
	}
	public function setBody(string $body){
	}
	public function getProperties(){
	}
	public function setProperties(array $properties){
	}
	public function toString(){
	}

}
