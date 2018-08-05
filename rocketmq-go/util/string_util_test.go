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

package util

import (
	"strings"
	"testing"
)

func TestStrToIntWithDefaultValue(t *testing.T) {
	result1 := StrToIntWithDefaultValue("1", 0)
	if result1 != 1 {
		t.Errorf("StrToIntWithDefaultValue failed : cannot transfer string 1 to int 1")
	}
	result2 := StrToIntWithDefaultValue("", 2)
	if result2 != 2 {
		t.Errorf("StrToIntWithDefaultValue failed : cannot use default value")
	}
}

func TestReadString(t *testing.T) {
	var i interface{} = "testReadString"
	ret := ReadString(i)
	if !strings.EqualFold(ret, "testReadString") {
		t.Errorf("TestReadString failed : cannot transfer string")
	}
	ret2 := ReadString(nil)
	if ret2 != "" {
		t.Errorf("TestReadString failed : cannot transfer nil to \"\"")
	}
}

func TestIntToString(t *testing.T) {
	ret := IntToString(1)
	if ret != "1" {
		t.Errorf("TestIntToString failed : cannot tansfer int 1 to  string1")
	}
}

func TestStrToInt32WithDefaultValue(t *testing.T) {
	i1 := int32(2147483646)
	i2 := int32(2147483647)
	result1 := StrToInt32WithDefaultValue("2147483646", i2)
	if result1 != i1 {
		t.Errorf("StrToIntWithDefaultValue failed : cannot transfer string 2147483646 to int32 2147483646")
	}
	result2 := StrToInt32WithDefaultValue("2147483648", i2)
	if result2 != i2 {
		t.Errorf("StrToIntWithDefaultValue failed : cannot use default value")
	}
}

func TestStrToInt64WithDefaultValue(t *testing.T) {
	i1 := int64(9223372036854775806)
	i2 := int64(9223372036854775807)
	result1 := StrToInt64WithDefaultValue("9223372036854775806", i2)
	if result1 != i1 {
		t.Errorf("StrToIntWithDefaultValue failed : cannot transfer string 9223372036854775806 to int64 9223372036854775806")
	}
	result2 := StrToInt64WithDefaultValue("9223372036854775808", i2)
	if result2 != i2 {
		t.Errorf("StrToIntWithDefaultValue failed : use default value")
	}
}
