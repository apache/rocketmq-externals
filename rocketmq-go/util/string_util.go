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
package util

import "strconv"

func StrToIntWithDefaultValue(str string, defaultValue int) (result int) {
	ret, err := strconv.Atoi(str)
	if err != nil {
		result = defaultValue
	} else {
		result = ret
	}
	return
}
func ReadString(obj interface{}) (ret string) {
	if obj == nil {
		ret = ""
	} else {
		ret = obj.(string)
	}
	return
}

func IntToString(intValue int) (ret string) {
	ret = strconv.Itoa(intValue)
	return
}

func StrToInt(str string) (result int, err error) {
	result, err = strconv.Atoi(str)
	return
}
func StrToInt32(str string) (result int32, err error) {
	var ret int64
	ret, err = strconv.ParseInt(str, 10, 32)
	result = int32(ret)
	return
}
func StrToInt16(str string) (result int16, err error) {
	var ret int64
	ret, err = strconv.ParseInt(str, 10, 16)
	result = int16(ret)
	return
}
func StrToInt64(str string) (result int64, err error) {
	result, err = strconv.ParseInt(str, 10, 64)
	return
}

func StrToInt32WithDefaultValue(str string, defaultValue int32) (result int32) {
	ret, err := StrToInt32(str)
	if err != nil {
		result = defaultValue
	} else {
		result = ret
	}
	return
}
func StrToInt16WithDefaultValue(str string, defaultValue int16) (result int16) {
	ret, err := StrToInt16(str)
	if err != nil {
		result = defaultValue
	} else {
		result = ret
	}
	return
}
func StrToInt64WithDefaultValue(str string, defaultValue int64) (result int64) {
	ret, err := StrToInt64(str)
	if err != nil {
		result = defaultValue
	} else {
		result = ret
	}
	return
}
