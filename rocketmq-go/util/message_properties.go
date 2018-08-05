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
)

//NAME_VALUE_SEPARATOR char 1 and 2 from java code
var NAME_VALUE_SEPARATOR = string(rune(1))

//PROPERTY_SEPARATOR property separator
var PROPERTY_SEPARATOR = string(rune(2))

//MessageProperties2String convert message properties to string
func MessageProperties2String(propertiesMap map[string]string) (ret string) {
	for key, value := range propertiesMap {
		ret = ret + key + NAME_VALUE_SEPARATOR + value + PROPERTY_SEPARATOR
	}
	return
}

//String2MessageProperties convert string properties to map
func String2MessageProperties(properties string) (ret map[string]string) {
	ret = make(map[string]string)
	for _, nameValueStr := range strings.Split(properties, PROPERTY_SEPARATOR) {
		nameValuePair := strings.Split(nameValueStr, NAME_VALUE_SEPARATOR)
		nameValueLen := len(nameValuePair)
		if nameValueLen != 2 {
			//glog.Error("nameValuePair is error", nameValueStr)
			continue
		}
		ret[nameValuePair[0]] = nameValuePair[1]
	}
	return
}
