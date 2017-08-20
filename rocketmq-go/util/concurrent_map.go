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
	"sync"
)

var MAP_ITEM_COUNT = 32

//ConcurrentMap
type ConcurrentMap []*concurrentMapItem

type concurrentMapItem struct {
	item map[string]interface{}
	sync.RWMutex
}

//create a ConcurrentMap instance
func New() ConcurrentMap {
	m := make(ConcurrentMap, MAP_ITEM_COUNT)
	for i := 0; i < MAP_ITEM_COUNT; i++ {
		m[i] = &concurrentMapItem{item: make(map[string]interface{})}
	}
	return m
}

func (m ConcurrentMap) GetMapItem(key string) *concurrentMapItem {
	return m[uint(fnv32(key))%uint(MAP_ITEM_COUNT)]
}

func (m *ConcurrentMap) Set(key string, value interface{}) {
	shard := m.GetMapItem(key)
	shard.Lock()
	shard.item[key] = value
	shard.Unlock()
}

func (m ConcurrentMap) Get(key string) (interface{}, bool) {
	shard := m.GetMapItem(key)
	shard.RLock()
	val, ok := shard.item[key]
	shard.RUnlock()
	return val, ok
}

// Returns the number of elements within the map.
func (m ConcurrentMap) Count() int {
	count := 0
	for i := 0; i < MAP_ITEM_COUNT; i++ {
		shard := m[i]
		shard.RLock()
		count += len(shard.item)
		shard.RUnlock()
	}
	return count
}

// Removes an element from the map.
func (m *ConcurrentMap) Remove(key string) {
	// Try to get shard.
	shard := m.GetMapItem(key)
	shard.Lock()
	delete(shard.item, key)
	shard.Unlock()
}

// Used by the Iter & IterBuffered functions to wrap two variables together over a channel,
type Tuple struct {
	Key string
	Val interface{}
}

// Returns a buffered iterator which could be used in a for range loop.
func (m ConcurrentMap) IterBuffered() <-chan Tuple {
	ch := make(chan Tuple, m.Count())
	go func() {
		wg := sync.WaitGroup{}
		wg.Add(MAP_ITEM_COUNT)
		// Foreach shard.
		for _, shard := range m {
			go func(shard *concurrentMapItem) {
				// Foreach key, value pair.
				shard.RLock()
				for key, val := range shard.item {
					ch <- Tuple{key, val}
				}
				shard.RUnlock()
				wg.Done()
			}(shard)
		}
		wg.Wait()
		close(ch)
	}()
	return ch
}

// Returns all item as map[string]interface{}
func (m ConcurrentMap) Items() map[string]interface{} {
	tmp := make(map[string]interface{})

	// Insert item to temporary map.
	for item := range m.IterBuffered() {
		tmp[item.Key] = item.Val
	}

	return tmp
}

// Return all keys as []string
func (m ConcurrentMap) Keys() []string {
	count := m.Count()
	ch := make(chan string, count)
	go func() {
		// Foreach shard.
		wg := sync.WaitGroup{}
		wg.Add(MAP_ITEM_COUNT)
		for _, shard := range m {
			go func(shard *concurrentMapItem) {
				// Foreach key, value pair.
				shard.RLock()
				for key := range shard.item {
					ch <- key
				}
				shard.RUnlock()
				wg.Done()
			}(shard)
		}
		wg.Wait()
		close(ch)
	}()

	keys := make([]string, 0, count)
	for k := range ch {
		keys = append(keys, k)
	}
	return keys
}

func fnv32(key string) uint32 {
	hash := uint32(2166136261)
	const prime32 = uint32(16777619)
	for i := 0; i < len(key); i++ {
		hash *= prime32
		hash ^= uint32(key[i])
	}
	return hash
}
