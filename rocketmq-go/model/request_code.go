/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http:// www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package model

const (
	// send message
	SendMsg = 10
	// subscribe message
	PullMsg = 11
	// query message
	QueryMessage = 12
	// queryOffset
	QueryBrokerOffset = 13
	// query Consumer Offset
	QueryConsumerOffset = 14
	// update Consumer Offset
	UpdateConsumerOffset = 15
	// update or increase a topic
	UpdateAndCreateTopic = 17
	// get all config of topic (Slave and Namesrv query the config to master)
	GetAllTopicConfig = 21
	// get all config (Slave and Namesrv query the config to master)
	GetTopicConfigList = 22
	// get all name list of topic
	GetTopicNameList = 23
	// update config
	UpdateBrokerConfig = 25
	// get config
	GetBrokerConfig = 26
	// trigger delete files
	TriggerDeleteFILES = 27
	// get runtime information
	GetBrokerRuntimeInfo = 28
	// search offset by timestamp
	SearchOffsetByTimeStamp = 29
	// query max offset of queue
	GetMaxOffset = 30
	// query min offset of queue
	GetMinOffset = 31
	// query earliest message store time
	GetEarliestMsgStoreTime = 32
	// query message by id
	ViewMsgById = 33
	// client send heartbeat to broker, and register self
	HeartBeat = 34
	// unregister client
	UnregisterClient = 35
	// consumer send message back to broker when can't process message
	ConsumerSendMsgBack = 36
	// Commit or Rollback transaction
	EndTransaction = 37
	// get ConsumerId list by GroupName
	GetConsumerListByGroup = 38
	// ckeck transaction state from producer
	CheckTransactionState = 39
	// broker notify consumer ids changed
	NotifyConsumerIdsChanged = 40
	// Consumer lock queue to master
	LockBatchMq = 41
	// Consumer unlock queue to master
	UNLockBatchMq = 42
	// get all consumer offset
	GetAllConsumerOffset = 43
	// get all delay offset
	GetAllDelayOffset = 45
	// put kv config to Namesrv
	PutKVConfig = 100
	// get kv config to Namesrv
	GetKVConfig = 101
	// delete  kv config to Namesrv
	DeleteKVConfig = 102
	// register a broker to Namesrv. As data is persistent,
	// the broker will overwrite if old config existed.
	RegisterBroker = 103
	// register a broker
	UnregisterBroker = 104
	// get broker name, queue numbers by topic.
	GetRouteinfoByTopic = 105
	// get all registered broker to namesrv info
	GetBrokerClusterInfo             = 106
	UpdateAndCreateSubscriptionGroup = 200
	GetAllSubscriptionGroupConfig    = 201
	GetTopicStatsInfo                = 202
	GetConsumerConnList              = 203
	GetProducerConnList              = 204
	WipeWritePermOfBroker            = 205

	// get all topic list from namesrv
	GetAllTopicListFromNamesrv = 206
	// delete subscription group from broker
	DeleteSubscriptionGroup = 207
	// get consume stats from broker
	GetConsumeStats = 208
	// Suspend Consumer
	SuspendConsumer = 209
	// Resume Consumer
	ResumeConsumer = 210
	// reset Consumer Offset
	ResetConsumerOffsetInConsumer = 211
	// reset Consumer Offset
	ResetConsumerOffsetInBroker = 212
	// query which consumer groups consume the msg
	WhoConsumeMessage = 214

	// namesrv delete topic config from broker
	DeleteTopicInBroker = 215
	// namesrv delete topic config from namesrv
	DeleteTopicInNamesrv = 216
	// namesrv get server ip info by project
	GetKvConfigByValue = 217
	// Namesrv delete all server ip by project group
	DeleteKvConfigByValue = 218
	// get all KV list by namespace
	GetKvlistByNamespace = 219

	// reset offset
	ResetConsumerClientOffset = 220
	// get consumer status from client
	GetConsumerStatusFromClient = 221
	// invoke broker to reset offset
	InvokeBrokerToResetOffset = 222
	// invoke broker to get consumer status
	InvokeBrokerToGetConsumerStatus = 223

	// query which consumer consume msg
	QueryTopicConsumeByWho = 300

	// get topics by cluster
	GetTopicsByCluster = 224

	// register filter server to broker
	RegisterFilterServer = 301
	// register class to filter server
	RegisterMsgFilterClass = 302
	// get time span by topic and group
	QueryConsumeTimeSpan = 303
	// get all system topics from namesrv
	GetSysTopicListFromNS = 304
	// get all system topics from broker
	GetSysTopicListFromBroker = 305

	// clean expired consume queue
	CleanExpiredConsumequeue = 306

	// query consumer memory data by broker
	GetConsumerRunningInfo = 307

	// TODO: query correction offset(transfer component?)
	QueryCorrectionOffset = 308

	// Send msg to one consumer by broker, The msg will immediately consume,
	// and return result to broker, broker return result to caller
	ConsumeMsgDirectly = 309

	// send msg with optimized network datagram
	SendMsgV2 = 310

	// get unit topic list
	GetUnitTopicList             = 311
	GetHasUnitSubTopicList       = 312
	GetHasUnitSubUnunitTopicList = 313
	CloneGroupOffset             = 314

	// query all status that broker count
	ViewBrokerStatsData = 315
)
