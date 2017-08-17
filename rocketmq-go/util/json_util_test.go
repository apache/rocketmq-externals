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

func TestGetKvStringMap(t *testing.T) {
	s1 := "{\"key1\":\"value1\",\"key2\":\"value2\"}"
	kvMap, err := GetKvStringMap(s1)
	if kvMap["\"key1\""] != "\"value1\"" || kvMap["\"key2\""] != "\"value2\"" || err != nil {
		t.Errorf("TestGetKvStringMap failed : cannot transfer normal json")
	}
	s2 := "\"key1\":\"value1\",\"key2\":\"value2\"}"
	kvMap2, err2 := GetKvStringMap(s2)
	if len(kvMap2) != 0 || err2.Error() != "json not start with {" {
		t.Errorf("TestGetKvStringMap failed : cannot found json error")
	}
	s3 := "{key1:value1,key2:value2}"
	kvMap3, err3 := GetKvStringMap(s3)
	if len(kvMap3) != 0 || err3.Error() != "INVALID JSON" {
		t.Errorf("TestGetKvStringMap failed : cannot found invalidjson")
	}
}
