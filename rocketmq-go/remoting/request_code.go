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

package remoting

const (
	//SEND_MESSAGE                SEND_MESSAGE
	SEND_MESSAGE = 10
	//PULL_MESSAGE                PULL_MESSAGE
	PULL_MESSAGE = 11
	//QUERY_CONSUMER_OFFSET       QUERY_CONSUMER_OFFSET
	QUERY_CONSUMER_OFFSET = 14
	//UPDATE_CONSUMER_OFFSET      UPDATE_CONSUMER_OFFSET
	UPDATE_CONSUMER_OFFSET = 15
	//SEARCH_OFFSET_BY_TIMESTAMP SEARCH_OFFSET_BY_TIMESTAMP
	SEARCH_OFFSET_BY_TIMESTAMP = 29
	//GET_MAX_OFFSET              GET_MAX_OFFSET
	GET_MAX_OFFSET = 30
	//HEART_BEAT                  HEART_BEAT
	HEART_BEAT = 34
	//CONSUMER_SEND_MSG_BACK      CONSUMER_SEND_MSG_BACK
	CONSUMER_SEND_MSG_BACK = 36
	//GET_CONSUMER_LIST_BY_GROUP  GET_CONSUMER_LIST_BY_GROUP
	GET_CONSUMER_LIST_BY_GROUP = 38
	//CHECK_TRANSACTION_STATE     CHECK_TRANSACTION_STATE
	CHECK_TRANSACTION_STATE = 39
	//NOTIFY_CONSUMER_IDS_CHANGED NOTIFY_CONSUMER_IDS_CHANGED
	NOTIFY_CONSUMER_IDS_CHANGED = 40
	//GET_ROUTEINTO_BY_TOPIC      GET_ROUTEINTO_BY_TOPIC
	GET_ROUTEINTO_BY_TOPIC = 105

	//RESET_CONSUMER_CLIENT_OFFSET RESET_CONSUMER_CLIENT_OFFSET
	RESET_CONSUMER_CLIENT_OFFSET = 220
	//GET_CONSUMER_STATUS_FROM_CLIENT GET_CONSUMER_STATUS_FROM_CLIENT
	GET_CONSUMER_STATUS_FROM_CLIENT = 221

	//GET_CONSUMER_RUNNING_INFO GET_CONSUMER_RUNNING_INFO
	GET_CONSUMER_RUNNING_INFO = 307

	//CONSUME_MESSAGE_DIRECTLY CONSUME_MESSAGE_DIRECTLY
	CONSUME_MESSAGE_DIRECTLY = 309
)
