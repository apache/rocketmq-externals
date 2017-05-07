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

package config

import (
	"bytes"
	"time"
)

// client common config
type ClientConfig struct {
	nameServerAddress string // only this is in use

	clientIP                      string
	instanceName                  string
	clientCallbackExecutorThreads int // TODO: clientCallbackExecutorThreads
	// Pulling topic information interval from the named server
	pullNameServerInterval time.Duration // default 30
	// Heartbeat interval in microseconds with message broker
	heartbeatBrokerInterval time.Duration // default 30
	// Offset persistent interval for consumer
	persistConsumerOffsetInterval time.Duration // default 5
	unitMode                      bool
	unitName                      string
	vipChannelEnabled             bool
}

func NewClientConfig() *ClientConfig {
	return &ClientConfig{
		unitMode:                      false,
		pullNameServerInterval:        time.Second * 30,
		heartbeatBrokerInterval:       time.Second * 30,
		persistConsumerOffsetInterval: time.Second * 30,
	}
}

func (config *ClientConfig) BuildMQClientId() string {
	var buffer bytes.Buffer
	buffer.WriteString(config.clientIP)
	buffer.WriteString("@")
	buffer.WriteString(config.instanceName)
	if config.unitName != "" {
		buffer.WriteString("@")
		buffer.WriteString(config.unitName)
	}
	return buffer.String()
}

func (config *ClientConfig) ChangeInstanceNameToPID() {
	// TODO
}

func (config *ClientConfig) ResetClientConfig(cfg *ClientConfig) {
	// TODO
}

func (config *ClientConfig) CloneClientConfig() *ClientConfig {
	return &ClientConfig{
		nameServerAddress:             config.nameServerAddress,
		clientIP:                      config.clientIP,
		instanceName:                  config.instanceName,
		clientCallbackExecutorThreads: config.clientCallbackExecutorThreads,
		pullNameServerInterval:        config.pullNameServerInterval,
		heartbeatBrokerInterval:       config.heartbeatBrokerInterval,
		persistConsumerOffsetInterval: config.persistConsumerOffsetInterval,
		unitMode:                      config.unitMode,
		unitName:                      config.unitName,
		vipChannelEnabled:             config.vipChannelEnabled,
	}
}

func (config *ClientConfig) ClientIP() string {
	return config.clientIP
}

func (config *ClientConfig) SetClientIP(s string) {
	config.clientIP = s
}

func (config *ClientConfig) InstanceName() string {
	return config.instanceName
}

func (config *ClientConfig) SetInstanceName(s string) {
	config.instanceName = s
}

func (config *ClientConfig) NameServerAddress() string {
	return config.nameServerAddress
}

func (config *ClientConfig) SetNameServerAddress(s string) {
	config.nameServerAddress = s
}

func (config *ClientConfig) ClientCallbackExecutorThreads() int {
	return config.clientCallbackExecutorThreads
}

func (config *ClientConfig) SetClientCallbackExecutorThreads(threads int) {
	config.clientCallbackExecutorThreads = threads
}

func (config *ClientConfig) PullNameServerInteval() time.Duration {
	return config.pullNameServerInterval
}

func (config *ClientConfig) SetPullNameServerInteval(interval time.Duration) {
	config.pullNameServerInterval = interval
}

func (config *ClientConfig) HeartbeatBrokerInterval() time.Duration {
	return config.heartbeatBrokerInterval
}

func (config *ClientConfig) SetHeartbeatBrokerInterval(interval time.Duration) {
	config.heartbeatBrokerInterval = interval
}

func (config *ClientConfig) PersistConsumerOffsetInterval() time.Duration {
	return config.persistConsumerOffsetInterval
}

func (config *ClientConfig) SetPersistConsumerOffsetInterval(interval time.Duration) {
	config.persistConsumerOffsetInterval = interval
}

func (config *ClientConfig) UnitName() string {
	return config.unitName
}

func (config *ClientConfig) SetUnitName(name string) {
	config.unitName = name
}

func (config *ClientConfig) UnitMode() bool {
	return config.unitMode
}

func (config *ClientConfig) SetUnitMode(mode bool) {
	config.unitMode = mode
}

func (config *ClientConfig) VipChannelEnabled() bool {
	return config.vipChannelEnabled
}

func (config *ClientConfig) SetVipChannelEnabled(enable bool) {
	config.vipChannelEnabled = enable
}

func (config *ClientConfig) String() string {
	//TODO
	return ""
}
