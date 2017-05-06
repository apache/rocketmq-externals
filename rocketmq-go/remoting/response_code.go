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
	SUCCESS                       = 0
	SYSTEM_ERROR                  = 1
	SYSTEM_BUSY                   = 2
	REQUEST_CODE_NOT_SUPPORTED    = 3
	TRANSACTION_FAILED            = 4
	FLUSH_DISK_TIMEOUT            = 10
	SLAVE_NOT_AVAILABLE           = 11
	FLUSH_SLAVE_TIMEOUT           = 12
	MESSAGE_ILLEGAL               = 13
	SERVICE_NOT_AVAILABLE         = 14
	VERSION_NOT_SUPPORTED         = 15
	NO_PERMISSION                 = 16
	TOPIC_NOT_EXIST               = 17
	TOPIC_EXIST_ALREADY           = 18
	PULL_NOT_FOUND                = 19
	PULL_RETRY_IMMEDIATELY        = 20
	PULL_OFFSET_MOVED             = 21
	QUERY_NOT_FOUND               = 22
	SUBSCRIPTION_PARSE_FAILED     = 23
	SUBSCRIPTION_NOT_EXIST        = 24
	SUBSCRIPTION_NOT_LATEST       = 25
	SUBSCRIPTION_GROUP_NOT_EXIST  = 26
	TRANSACTION_SHOULD_COMMIT     = 200
	TRANSACTION_SHOULD_ROLLBACK   = 201
	TRANSACTION_STATE_UNKNOW      = 202
	TRANSACTION_STATE_GROUP_WRONG = 203
	NO_BUYER_ID                   = 204

	NOT_IN_CURRENT_UNIT = 205

	CONSUMER_NOT_ONLINE = 206

	CONSUME_MSG_TIMEOUT = 207
)
