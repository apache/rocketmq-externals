/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package constant

const (
	//MESSAGE_COMPRESS_LEVEL      MESSAGE_COMPRESS_LEVEL
	MESSAGE_COMPRESS_LEVEL = "rocketmq.message.compressLevel"
	//DEFAULT_TOPIC               DEFAULT_TOPIC
	DEFAULT_TOPIC = "TBW102"
	//CLIENT_INNER_PRODUCER_GROUP CLIENT_INNER_PRODUCER_GROUP
	CLIENT_INNER_PRODUCER_GROUP = "CLIENT_INNER_PRODUCER"

	//MASTER_ID MASTER_ID
	MASTER_ID int64 = 0
	//CURRENT_JVM_PID CURRENT_JVM_PID
	CURRENT_JVM_PID
	//RETRY_GROUP_TOPIC_PREFIX RETRY_GROUP_TOPIC_PREFIX
	RETRY_GROUP_TOPIC_PREFIX = "%RETRY%"
	//DLQ_GROUP_TOPIC_PREFIX     DLQ_GROUP_TOPIC_PREFIX
	DLQ_GROUP_TOPIC_PREFIX = "%DLQ%"
	//SYSTEM_TOPIC_PREFIX        SYSTEM_TOPIC_PREFIX
	SYSTEM_TOPIC_PREFIX = "rmq_sys_"
	//UNIQUE_MSG_QUERY_FLAG      UNIQUE_MSG_QUERY_FLAG
	UNIQUE_MSG_QUERY_FLAG = "_UNIQUE_KEY_QUERY"
	//MAX_MESSAGE_BODY_SIZE  MAX_MESSAGE_BODY_SIZE
	MAX_MESSAGE_BODY_SIZE int = 4 * 1024 * 1024 //4m
	//MAX_MESSAGE_TOPIC_SIZE MAX_MESSAGE_TOPIC_SIZE
	MAX_MESSAGE_TOPIC_SIZE int = 255 //255char
	//DEFAULT_TOPIC_QUEUE_NUMS DEFAULT_TOPIC_QUEUE_NUMS
	DEFAULT_TOPIC_QUEUE_NUMS int32 = 4
)
