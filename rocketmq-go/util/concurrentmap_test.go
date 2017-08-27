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

func TestGetAndSet(t *testing.T) {
	existKey := "123"
	existValue := 1
	notExistKey := "321"
	concurrentMap := NewConcurrentMap()
	concurrentMap.Set(existKey, existValue)
	value, isHave := concurrentMap.Get(existKey)
	if !isHave {
		t.Errorf("can't read existKey")
	}
	if value != existValue {
		t.Errorf("get value fail")
	}
	value2, isHave2 := concurrentMap.Get(notExistKey)
	if isHave2 {
		t.Errorf("read notExistKey")
	}
	if value2 != nil {
		t.Errorf("read notExistKey error")
	}
}
func TesCount(t *testing.T) {
	count := 123
	concurrentMap := NewConcurrentMap()
	for i := 0; i < count; i++ {
		concurrentMap.Set(IntToString(i), "")
	}
	if concurrentMap.Count() != count {
		t.Errorf("Count error")
	}
}
func TesRemove(t *testing.T) {
	count := 123
	removeKey := "111"
	concurrentMap := NewConcurrentMap()
	for i := 0; i < count; i++ {
		concurrentMap.Set(IntToString(i), "")
	}
	concurrentMap.Remove(removeKey)
	if concurrentMap.Count() != count-1 {
		t.Errorf("remove error")
	}
	_, isHave := concurrentMap.Get(removeKey)
	if isHave {
		t.Errorf("remove error")

	}
}
func TesItems(t *testing.T) {
	count := 123
	concurrentMap := NewConcurrentMap()
	for i := 0; i < count; i++ {
		concurrentMap.Set(IntToString(i), "")
	}
	itemMap := concurrentMap.Items()
	if len(itemMap) != count {
		t.Errorf("Items error")
	}

}
func TesKeys(t *testing.T) {
	count := 123
	concurrentMap := NewConcurrentMap()
	for i := 0; i < count; i++ {
		concurrentMap.Set(IntToString(i), "")
	}
	keys := concurrentMap.Keys()
	if len(keys) != count {
		t.Errorf("keys error")
	}

}
func TesValues(t *testing.T) {
	count := 123
	concurrentMap := NewConcurrentMap()
	for i := 0; i < count; i++ {
		concurrentMap.Set(IntToString(i), "")
	}
	values := concurrentMap.Values()
	if len(values) != count {
		t.Errorf("values error")
	}
}
