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
package consumer

const (
	FlagCommitOffset int = 0x1 << 0
	FlagSuspend      int = 0x1 << 1
	FlagSubscription int = 0x1 << 2
	FlagClassFilter  int = 0x1 << 3
)

func BuildSysFlag(commitOffset, suspend, subsription, classFillter bool) int {
	var flag int = 0
	if commitOffset {
		flag |= FlagCommitOffset
	}

	if suspend {
		flag |= FlagSuspend
	}

	if subsription {
		flag |= FlagSubscription
	}

	if classFillter {
		flag |= FlagClassFilter
	}

	return flag
}

func ClearCommitOffsetFlag(sysFlag int) int {
	return sysFlag & (^FlagCommitOffset)
}

func HasClassFilterFlag(sysFlag int) bool {
	return (sysFlag & FlagClassFilter) == FlagClassFilter
}
