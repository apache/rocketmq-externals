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
namespace RocketMQ\Client\Latency;

class MQFaultStrategy
{
	public $log;
	public $latencyFaultTolerance;
	public $sendLatencyFaultEnable = false;
	public $latencyMax = [50, 100, 550, 1000, 2000, 3000, 15000];
	public $notAvailableDuration = [0, 0, 30000, 60000, 120000, 180000, 600000];
	public function __construct()
	{
		$this->log = ClientLogger.getLog();
		$this->latencyFaultTolerance = new LatencyFaultToleranceImpl();
	}
	public function getNotAvailableDuration()
	{
		return $this->notAvailableDuration;
	}
	public function setNotAvailableDuration($notAvailableDuration)
	{
		$this->notAvailableDuration = $notAvailableDuration;
	}
	
	public function getLatencyMax()
	{
		return $this->latencyMax;
	}
	
	public function setLatencyMax($latencyMax)
	{
		$this->latencyMax = $latencyMax;
	}
	
	public function isSendLatencyFaultEnable()
	{
		return $this->sendLatencyFaultEnable;
	}
	public function setSendLatencyFaultEnable($sendLatencyFaultEnable)
	{
		$this->sendLatencyFaultEnable = $sendLatencyFaultEnable;
	}
	public function selectOneMessageQueue($tpInfo, $lastBrokerName)
	{
		if ($this->sendLatencyFaultEnable) {
			try {
				$index = $tpInfo->getSendWhichQueue()->getAndIncrement();
				for ($i = 0; $i < strlen($tpInfo->getMessageQueueList()); $i++) {
					$pos = abs($index++) % strlen(tpInfo.getMessageQueueList());
					if ($pos < 0)
															                        $pos = 0;
					$mq = $tpInfo->getMessageQueueList()->get($pos);
					if ($this->latencyFaultTolerance->isAvailable($mq->getBrokerName())) {
						if (null == $lastBrokerName || $mq->getBrokerName() == $lastBrokerName)
																		                            return $mq;
					}
				}
				
				$notBestBroker = $this->latencyFaultTolerance->pickOneAtLeast();
				$writeQueueNums = $tpInfo->getQueueIdByBroker($notBestBroker);
				if ($writeQueueNums > 0) {
					$mq = $tpInfo->selectOneMessageQueue();
					if ($notBestBroker != null) {
						$mq->setBrokerName($notBestBroker);
						$mq->setQueueId($tpInfo->getSendWhichQueue()->getAndIncrement() % $writeQueueNums);
					}
					return $mq;
				}
				else {
					$this->latencyFaultTolerance->remove($notBestBroker);
				}
			}
			catch (\Exception $e) {
				$this->log->error("Error occurred when selecting message queue", e);
			}
			
			return $this->tpInfo->selectOneMessageQueue();
		}
		
		return $this->tpInfo->selectOneMessageQueue($lastBrokerName);
	}
	public function updateFaultItem($brokerName, $currentLatency, $isolation)
	{
		
	}
	
	public function computeNotAvailableDuration($currentLatency)
	{
		
	}
}
