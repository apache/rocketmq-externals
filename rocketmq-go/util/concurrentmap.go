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
	"hash/fnv"
	"sync"
)

//ConcurrentMap concurrent map
type ConcurrentMap interface {
	Get(key string) (interface{}, bool)
	Set(key string, value interface{})
	Count() int
	Remove(key string)
	Items() map[string]interface{}
	Keys() []string
	Values() []interface{}
}

//defaultMapSegmentCount default is 33,because map's default is 32
//you can use NewConcurrentMapWithSegmentCount to change the segment count(it depend on your map size)
var defaultMapSegmentCount = 33

//concurrentMapImpl
type concurrentMapImpl struct {
	segments     []*concurrentMapSegment
	segmentCount int
}

type concurrentMapSegment struct {
	item map[string]interface{}
	sync.RWMutex
}

//NewConcurrentMap create a concurrentMap instance with default segments count
func NewConcurrentMap() (concurrentMap ConcurrentMap) {
	return NewConcurrentMapWithSegmentCount(defaultMapSegmentCount)
}

//NewConcurrentMapWithSegmentCount create a concurrentMap instance with segments count
func NewConcurrentMapWithSegmentCount(segmentCount int) (concurrentMap ConcurrentMap) {
	concurrentMapSegments := make([]*concurrentMapSegment, segmentCount)
	for i := 0; i < segmentCount; i++ {
		concurrentMapSegments[i] = &concurrentMapSegment{item: make(map[string]interface{})}
	}
	concurrentMap = &concurrentMapImpl{segments: concurrentMapSegments, segmentCount: segmentCount}
	return
}

//Get get map[key]
func (m concurrentMapImpl) Get(key string) (interface{}, bool) {
	segment := m.getMapSegment(key)
	segment.RLock()
	val, ok := segment.item[key]
	segment.RUnlock()
	return val, ok
}

func (m concurrentMapImpl) getMapSegment(key string) *concurrentMapSegment {
	h := fnv.New32a()
	h.Write([]byte(key))
	segmentIndex := uint(h.Sum32() % uint32(m.segmentCount))
	return m.segments[segmentIndex]
}

//Set set map[key] = value
func (m *concurrentMapImpl) Set(key string, value interface{}) {
	segment := m.getMapSegment(key)
	segment.Lock()
	segment.item[key] = value
	segment.Unlock()
}

//Count count the number of items in this map.
func (m concurrentMapImpl) Count() int {
	count := 0
	for i := 0; i < m.segmentCount; i++ {
		segment := m.segments[i]
		segment.RLock()
		count += len(segment.item)
		segment.RUnlock()
	}
	return count
}

//Remove remove item by key
func (m *concurrentMapImpl) Remove(key string) {
	segment := m.getMapSegment(key)
	segment.Lock()
	delete(segment.item, key)
	segment.Unlock()
}

//Items put all item into one map
func (m concurrentMapImpl) Items() map[string]interface{} {
	tmp := make(map[string]interface{})
	for item := range m.allMapEntry() {
		tmp[item.Key] = item.Value
	}
	return tmp
}

//Keys all keys in this concurrent map
func (m concurrentMapImpl) Keys() []string {
	allMapEntry := m.allMapEntry()
	keys := make([]string, 0, len(allMapEntry))
	for entry := range allMapEntry {
		keys = append(keys, entry.Key)
	}
	return keys
}

//Values all values in this concurrent map
func (m concurrentMapImpl) Values() []interface{} {
	allMapEntry := m.allMapEntry()
	values := make([]interface{}, 0, len(allMapEntry))
	for entry := range allMapEntry {
		values = append(values, entry.Value)
	}
	return values
}

//MapEntry map entry,has key and value
type MapEntry struct {
	Key   string
	Value interface{}
}

func (m concurrentMapImpl) allMapEntry() <-chan MapEntry {
	ch := make(chan MapEntry, m.Count())
	go func() {
		wg := sync.WaitGroup{}
		wg.Add(m.segmentCount)
		for _, segment := range m.segments {
			go func(segment *concurrentMapSegment) {
				segment.RLock()
				for key, val := range segment.item {
					ch <- MapEntry{key, val}
				}
				segment.RUnlock()
				wg.Done()
			}(segment)
		}
		wg.Wait()
		close(ch)
	}()
	return ch
}
