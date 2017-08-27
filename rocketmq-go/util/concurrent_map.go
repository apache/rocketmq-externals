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

//import (
//	"sync"
//	"hash/fnv"
//)
//
//
//
////default_map_segment_count
//var default_map_segment_count = 33
//
////ConcurrentMap
//type ConcurrentMap []*concurrentMapSegment
//
//type concurrentMapSegment struct {
//	item map[string]interface{}
//	sync.RWMutex
//}
//
////NewConcurrentMap create a ConcurrentMap instance
//func NewConcurrentMap() ConcurrentMap {
//	m := make(ConcurrentMap, default_map_segment_count)
//	for i := 0; i < default_map_segment_count; i++ {
//		m[i] = &concurrentMapSegment{item: make(map[string]interface{})}
//	}
//	return m
//}
//
//func (m ConcurrentMap) getMapSegment(key string) *concurrentMapSegment {
//	return m[segmentIndex(key)]
//}
//
//func (m *ConcurrentMap) Set(key string, value interface{}) {
//	shard := m.getMapSegment(key)
//	shard.Lock()
//	shard.item[key] = value
//	shard.Unlock()
//}
//
//func (m ConcurrentMap) Get(key string) (interface{}, bool) {
//	shard := m.getMapSegment(key)
//	shard.RLock()
//	val, ok := shard.item[key]
//	shard.RUnlock()
//	return val, ok
//}
//
//// Returns the number of elements within the map.
//func (m ConcurrentMap) Count() int {
//	count := 0
//	for i := 0; i < default_map_segment_count; i++ {
//		shard := m[i]
//		shard.RLock()
//		count += len(shard.item)
//		shard.RUnlock()
//	}
//	return count
//}
//
//// Removes an element from the map.
//func (m *ConcurrentMap) Remove(key string) {
//	// Try to get shard.
//	shard := m.getMapSegment(key)
//	shard.Lock()
//	delete(shard.item, key)
//	shard.Unlock()
//}
//
//// Used by the Iter & allMapEntry functions to wrap two variables together over a channel,
//type MapEntry struct {
//	Key string
//	Value interface{}
//}
//
//// Returns a buffered allMapEntry which could be used in a for range loop.
//func (m ConcurrentMap) allMapEntry() <-chan MapEntry {
//	ch := make(chan MapEntry, m.Count())
//	go func() {
//		wg := sync.WaitGroup{}
//		wg.Add(default_map_segment_count)
//		// Foreach shard.
//		for _, shard := range m {
//			go func(shard *concurrentMapSegment) {
//				// Foreach key, value pair.
//				shard.RLock()
//				for key, val := range shard.item {
//					ch <- MapEntry{key, val}
//				}
//				shard.RUnlock()
//				wg.Done()
//			}(shard)
//		}
//		wg.Wait()
//		close(ch)
//	}()
//	return ch
//}
//
//// Returns all item as map[string]interface{}
//func (m ConcurrentMap) Items() map[string]interface{} {
//	tmp := make(map[string]interface{})
//
//	// Insert item to temporary map.
//	for item := range m.allMapEntry() {
//		tmp[item.Key] = item.Value
//	}
//
//	return tmp
//}
//
//// Return all keys as []string
//func (m ConcurrentMap) Keys() []string {
//	count := m.Count()
//	ch := make(chan string, count)
//	go func() {
//		// Foreach shard.
//		wg := sync.WaitGroup{}
//		wg.Add(default_map_segment_count)
//		for _, shard := range m {
//			go func(shard *concurrentMapSegment) {
//				// Foreach key, value pair.
//				shard.RLock()
//				for key := range shard.item {
//					ch <- key
//				}
//				shard.RUnlock()
//				wg.Done()
//			}(shard)
//		}
//		wg.Wait()
//		close(ch)
//	}()
//
//	keys := make([]string, 0, count)
//	for k := range ch {
//		keys = append(keys, k)
//	}
//	return keys
//}
//func segmentIndex(key string) uint {
//	h := fnv.New32a()
//	h.Write([]byte(key))
//	return uint(h.Sum32()) % uint(default_map_segment_count)
//}
