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
	"testing"
)

func TestMessageProperties2String(t *testing.T) {
	stringMap := make(map[string]string)
	stringMap["key1"] = "value1"
	stringMap["key2"] = "value2"
	var NAME_VALUE_SEPARATOR = string(rune(1))
	var PROPERTY_SEPARATOR = string(rune(2))
	s1 := "key1" + NAME_VALUE_SEPARATOR + "value1" + PROPERTY_SEPARATOR + "key2" +
		NAME_VALUE_SEPARATOR + "value2" + PROPERTY_SEPARATOR
	ret := MessageProperties2String(stringMap)
	if ret != s1 {
		t.Errorf("TestMessageProperties2String failed")
	}
}

func TestString2MessageProperties(t *testing.T) {
	var NAME_VALUE_SEPARATOR = string(rune(1))
	var PROPERTY_SEPARATOR = string(rune(2))
	s1 := "key1" + NAME_VALUE_SEPARATOR + "value1" + PROPERTY_SEPARATOR + "key2" +
		NAME_VALUE_SEPARATOR + "value2"
	mapString := String2MessageProperties(s1)
	if mapString["key1"] != "value1" || mapString["key2"] != "value2" {
		t.Errorf("TestString2MessageProperties failed")
	}
}
