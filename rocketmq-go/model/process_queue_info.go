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
package model

type ProcessQueueInfo struct {
	CommitOffset int64 `json:"commitOffset"`

	CachedMsgMinOffset int64 `json:"cachedMsgMinOffset"`
	CachedMsgMaxOffset int64 `json:"cachedMsgMaxOffset"`
	CachedMsgCount     int32 `json:"cachedMsgCount"`

	TransactionMsgMinOffset int64 `json:"transactionMsgMinOffset"`
	TransactionMsgMaxOffset int64 `json:"transactionMsgMaxOffset"`
	TransactionMsgCount     int32 `json:"transactionMsgCount"`

	Locked            bool  `json:"locked"`
	TryUnlockTimes    int64 `json:"tryUnlockTimes"`
	LastLockTimestamp int64 `json:"lastLockTimestamp"`

	Droped               bool  `json:"droped"`
	LastPullTimestamp    int64 `json:"lastPullTimestamp"`
	LastConsumeTimestamp int64 `json:"lastConsumeTimestamp"`
}

//func (self ProcessQueueInfo) BuildFromProcessQueue(processQueue ProcessQueue) (processQueueInfo ProcessQueueInfo) {
//	processQueueInfo = ProcessQueueInfo{}
//	//processQueueInfo.CommitOffset =
//	processQueueInfo.CachedMsgCount = processQueue.GetMsgCount()
//	processQueueInfo.CachedMsgCount
//	return
//}
