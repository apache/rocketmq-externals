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

func TestMatchString(t *testing.T) {
	s1 := "123"
	s2 := "123qwe"
	pattern := "^[0-9]+$"
	notPattern := "\\123\\"
	result1 := MatchString(s1, pattern)
	result2 := MatchString(s2, pattern)
	if !result1 || result2 {
		t.Errorf("TestMatchString failed : cannot match")
	}
	result3 := MatchString(s1, notPattern)
	if result3 {
		t.Errorf("TestMatchString failed : cannot find pattern mistake")
	}
}
