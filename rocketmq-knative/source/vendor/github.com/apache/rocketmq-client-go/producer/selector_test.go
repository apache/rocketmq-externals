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

package producer

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/apache/rocketmq-client-go/primitive"
)

func TestRoundRobin(t *testing.T) {
	queues := make([]*primitive.MessageQueue, 10)
	for i := 0; i < 10; i++ {
		queues = append(queues, &primitive.MessageQueue{
			QueueId: i,
		})
	}
	s := NewRoundRobinQueueSelector()

	m := &primitive.Message{
		Topic: "test",
	}
	mrr := &primitive.Message{
		Topic: "rr",
	}
	for i := 0; i < 100; i++ {
		q := s.Select(m, queues)
		expected := (i + 1) % len(queues)
		assert.Equal(t, queues[expected], q, "i: %d", i)

		qrr := s.Select(mrr, queues)
		expected = (i + 1) % len(queues)
		assert.Equal(t, queues[expected], qrr, "i: %d", i)
	}
}

func TestHashQueueSelector(t *testing.T) {
	queues := make([]*primitive.MessageQueue, 10)
	for i := 0; i < 10; i++ {
		queues = append(queues, &primitive.MessageQueue{
			QueueId: i,
		})
	}

	s := NewHashQueueSelector()

	m1 := &primitive.Message{
		Topic: "test",
		Body:  []byte("one message"),
	}
	m1.WithShardingKey("same_key")
	q1 := s.Select(m1, queues)

	m2 := &primitive.Message{
		Topic: "test",
		Body:  []byte("another message"),
	}
	m2.WithShardingKey("same_key")
	q2 := s.Select(m2, queues)
	assert.Equal(t, *q1, *q2)
}
