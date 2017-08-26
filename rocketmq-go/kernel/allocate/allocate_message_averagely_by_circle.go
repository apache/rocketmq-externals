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

package allocate

import (
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
)

//AllocateMessageQueueAveragelyByCircle AllocateMessageQueueAveragelyByCircle
type AllocateMessageQueueAveragelyByCircle struct{}

//Allocate message queue
func (a *AllocateMessageQueueAveragelyByCircle) Allocate(consumerGroup string, currentCID string, mqAll []*rocketmqm.MessageQueue, cidAll []string) ([]rocketmqm.MessageQueue, error) {
	if currentCID == "" {
		return nil, errors.New("currentCID is empty")
	}

	if mqAll == nil || len(mqAll) == 0 {
		return nil, errors.New("mqAll is nil or mqAll empty")
	}

	if cidAll == nil || len(cidAll) == 0 {
		return nil, errors.New("cidAll is nil or cidAll empty")
	}

	result := make([]rocketmqm.MessageQueue, 0)
	for i, cid := range cidAll {
		if cid == currentCID {
			mqLen := len(mqAll)
			cidLen := len(cidAll)
			mod := mqLen % cidLen
			var averageSize int
			if mqLen < cidLen {
				averageSize = 1
			} else {
				if mod > 0 && i < mod {
					averageSize = mqLen/cidLen + 1
				} else {
					averageSize = mqLen / cidLen
				}
			}

			var startIndex int
			if mod > 0 && i < mod {
				startIndex = i * averageSize
			} else {
				startIndex = i*averageSize + mod
			}

			var min int
			if averageSize > mqLen-startIndex {
				min = mqLen - startIndex
			} else {
				min = averageSize
			}

			for j := 0; j < min; j++ {
				result = append(result, *mqAll[(startIndex+j)%mqLen])
			}
			return result, nil

		}
	}

	return nil, errors.New("cant't find currentCID")
}
