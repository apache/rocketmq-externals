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
package constant

const (
	ROCKETMQ_HOME_ENV      = "ROCKETMQ_HOME"
	ROCKETMQ_HOME_PROPERTY = "rocketmq.home.dir"
	NAMESRV_ADDR_ENV       = "NAMESRV_ADDR"
	NAMESRV_ADDR_PROPERTY  = "rocketmq.namesrv.addr"
	MESSAGE_COMPRESS_LEVEL = "rocketmq.message.compressLevel"
	//WS_DOMAIN_NAME = System.getProperty("rocketmq.namesrv.domain", "jmenv.tbsite.net")
	//WS_DOMAIN_SUBGROUP = System.getProperty("rocketmq.namesrv.domain.subgroup", "nsaddr")
	// http://jmenv.tbsite.net:8080/rocketmq/nsaddr
	//WS_ADDR = "http://" + WS_DOMAIN_NAME + ":8080/rocketmq/" + WS_DOMAIN_SUBGROUP
	DEFAULT_TOPIC               = "TBW102"
	BENCHMARK_TOPIC             = "BenchmarkTest"
	DEFAULT_PRODUCER_GROUP      = "DEFAULT_PRODUCER"
	DEFAULT_CONSUMER_GROUP      = "DEFAULT_CONSUMER"
	TOOLS_CONSUMER_GROUP        = "TOOLS_CONSUMER"
	FILTERSRV_CONSUMER_GROUP    = "FILTERSRV_CONSUMER"
	MONITOR_CONSUMER_GROUP      = "__MONITOR_CONSUMER"
	CLIENT_INNER_PRODUCER_GROUP = "CLIENT_INNER_PRODUCER"
	SELF_TEST_PRODUCER_GROUP    = "SELF_TEST_P_GROUP"
	SELF_TEST_CONSUMER_GROUP    = "SELF_TEST_C_GROUP"
	SELF_TEST_TOPIC             = "SELF_TEST_TOPIC"
	OFFSET_MOVED_EVENT          = "OFFSET_MOVED_EVENT"
	ONS_HTTP_PROXY_GROUP        = "CID_ONS-HTTP-PROXY"
	CID_ONSAPI_PERMISSION_GROUP = "CID_ONSAPI_PERMISSION"
	CID_ONSAPI_OWNER_GROUP      = "CID_ONSAPI_OWNER"
	CID_ONSAPI_PULL_GROUP       = "CID_ONSAPI_PULL"
	CID_RMQ_SYS_PREFIX          = "CID_RMQ_SYS_"

	//public static final List<String> LocalInetAddrs = getLocalInetAddress()
	//Localhost = localhost()
	//DEFAULT_CHARSET = "UTF-8"
	MASTER_ID int64 = 0
	CURRENT_JVM_PID

	RETRY_GROUP_TOPIC_PREFIX = "%RETRY%"

	DLQ_GROUP_TOPIC_PREFIX     = "%DLQ%"
	SYSTEM_TOPIC_PREFIX        = "rmq_sys_"
	UNIQUE_MSG_QUERY_FLAG      = "_UNIQUE_KEY_QUERY"
	MAX_MESSAGE_BODY_SIZE  int = 4 * 1024 * 1024 //4m
	MAX_MESSAGE_TOPIC_SIZE int = 255             //255char

	DEFAULT_TOPIC_QUEUE_NUMS int32 = 4
)
