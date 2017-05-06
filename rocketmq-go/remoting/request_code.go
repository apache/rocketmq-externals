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
package remoting

const (
	SEND_MESSAGE                        = 10
	PULL_MESSAGE                        = 11
	QUERY_MESSAGE                       = 12
	QUERY_BROKER_OFFSET                 = 13
	QUERY_CONSUMER_OFFSET               = 14
	UPDATE_CONSUMER_OFFSET              = 15
	UPDATE_AND_CREATE_TOPIC             = 17
	GET_ALL_TOPIC_CONFIG                = 21
	GET_TOPIC_CONFIG_LIST               = 22
	GET_TOPIC_NAME_LIST                 = 23
	UPDATE_BROKER_CONFIG                = 25
	GET_BROKER_CONFIG                   = 26
	TRIGGER_DELETE_FILES                = 27
	GET_BROKER_RUNTIME_INFO             = 28
	SEARCH_OFFSET_BY_TIMESTAMP          = 29
	GET_MAX_OFFSET                      = 30
	GET_MIN_OFFSET                      = 31
	GET_EARLIEST_MSG_STORETIME          = 32
	VIEW_MESSAGE_BY_ID                  = 33
	HEART_BEAT                          = 34
	UNREGISTER_CLIENT                   = 35
	CONSUMER_SEND_MSG_BACK              = 36
	END_TRANSACTION                     = 37
	GET_CONSUMER_LIST_BY_GROUP          = 38
	CHECK_TRANSACTION_STATE             = 39
	NOTIFY_CONSUMER_IDS_CHANGED         = 40
	LOCK_BATCH_MQ                       = 41
	UNLOCK_BATCH_MQ                     = 42
	GET_ALL_CONSUMER_OFFSET             = 43
	GET_ALL_DELAY_OFFSET                = 45
	PUT_KV_CONFIG                       = 100
	GET_KV_CONFIG                       = 101
	DELETE_KV_CONFIG                    = 102
	REGISTER_BROKER                     = 103
	UNREGISTER_BROKER                   = 104
	GET_ROUTEINTO_BY_TOPIC              = 105
	GET_BROKER_CLUSTER_INFO             = 106
	UPDATE_AND_CREATE_SUBSCRIPTIONGROUP = 200
	GET_ALL_SUBSCRIPTIONGROUP_CONFIG    = 201
	GET_TOPIC_STATS_INFO                = 202
	GET_CONSUMER_CONNECTION_LIST        = 203
	GET_PRODUCER_CONNECTION_LIST        = 204
	WIPE_WRITE_PERM_OF_BROKER           = 205

	GET_ALL_TOPIC_LIST_FROM_NAMESERVER = 206
	DELETE_SUBSCRIPTIONGROUP           = 207
	GET_CONSUME_STATS                  = 208
	SUSPEND_CONSUMER                   = 209
	RESUME_CONSUMER                    = 210
	RESET_CONSUMER_OFFSET_IN_CONSUMER  = 211
	RESET_CONSUMER_OFFSET_IN_BROKER    = 212
	ADJUST_CONSUMER_THREAD_POOL        = 213
	WHO_CONSUME_THE_MESSAGE            = 214

	DELETE_TOPIC_IN_BROKER    = 215
	DELETE_TOPIC_IN_NAMESRV   = 216
	GET_KV_CONFIG_BY_VALUE    = 217
	DELETE_KV_CONFIG_BY_VALUE = 218
	GET_KVLIST_BY_NAMESPACE   = 219

	RESET_CONSUMER_CLIENT_OFFSET         = 220
	GET_CONSUMER_STATUS_FROM_CLIENT      = 221
	INVOKE_BROKER_TO_RESET_OFFSET        = 222
	INVOKE_BROKER_TO_GET_CONSUMER_STATUS = 223

	QUERY_TOPIC_CONSUME_BY_WHO = 300

	GET_TOPICS_BY_CLUSTER = 224

	REGISTER_FILTER_SERVER            = 301
	REGISTER_MESSAGE_FILTER_CLASS     = 302
	QUERY_CONSUME_TIME_SPAN           = 303
	GET_SYSTEM_TOPIC_LIST_FROM_NS     = 304
	GET_SYSTEM_TOPIC_LIST_FROM_BROKER = 305

	CLEAN_EXPIRED_CONSUMEQUEUE = 306

	GET_CONSUMER_RUNNING_INFO = 307

	QUERY_CORRECTION_OFFSET = 308

	CONSUME_MESSAGE_DIRECTLY = 309

	SEND_MESSAGE_V2 = 310

	GET_UNIT_TOPIC_LIST                = 311
	GET_HAS_UNIT_SUB_TOPIC_LIST        = 312
	GET_HAS_UNIT_SUB_UNUNIT_TOPIC_LIST = 313
	CLONE_GROUP_OFFSET                 = 314

	VIEW_BROKER_STATS_DATA = 315
)
