/*
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
package service

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"strconv"
	"os"
	"time"
	"github.com/golang/glog"
)
var waitInterval time.Duration

func init() {
	interval, err := strconv.Atoi(os.Getenv("rocketmq.client.rebalance.waitInterval"))
	if err != nil {
		waitInterval = 20 * time.Second
		glog.Warningf("rocketmq.client.rebalance.waitInterval unset!")
	} else {
		waitInterval = time.Duration(interval) * time.Millisecond
	}
}

type AllocateMessageQueueStrategy interface {
	Allocate(consumeGroup, currentCID string, mqAll []*message.MessageQueue, CIDAll []string) []*message.MessageQueue
	StrategyName() string
}

type RebalanceService struct {
	mqClient *MQClient

	quit chan bool
}

func (rb *RebalanceService) Start() {
	go rb.run()
	glog.Info("RocketMQ Client Rebalance Service STARTED!")
}

func (rb *RebalanceService) Shutdown() {
	rb.quit <- true
	glog.Info("RocketMQ Client Rebalance Service SHUTDOWN!")
}

func (rb *RebalanceService) run() {
	timer := time.NewTimer(waitInterval)
	for {
		select {
		case timer.C:
			rb.mqClient.DoRebalance()
		case <- rb.quit:
			return
		}
	}
}

