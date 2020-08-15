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

package main

import (
	"context"
	"fmt"
	"time"

	"github.com/apache/rocketmq-client-go"
	"github.com/apache/rocketmq-client-go/consumer"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
)

func main() {
	c, err := rocketmq.NewPullConsumer(
		consumer.WithGroupName("testGroup"),
		consumer.WithNameServer([]string{"127.0.0.1:9876"}),
	)
	if err != nil {
		rlog.Fatal(fmt.Sprintf("fail to new pullConsumer: %s", err), nil)
	}
	err = c.Start()
	if err != nil {
		rlog.Fatal(fmt.Sprintf("fail to new pullConsumer: %s", err), nil)
	}

	ctx := context.Background()
	queue := primitive.MessageQueue{
		Topic:      "TopicTest",
		BrokerName: "", // replace with your broker name. otherwise, pull will failed.
		QueueId:    0,
	}

	offset := int64(0)
	for {
		resp, err := c.PullFrom(ctx, queue, offset, 10)
		if err != nil {
			if err == rocketmq.ErrRequestTimeout {
				fmt.Printf("timeout \n")
				time.Sleep(1 * time.Second)
				continue
			}
			fmt.Printf("unexpectable err: %v \n", err)
			return
		}
		if resp.Status == primitive.PullFound {
			fmt.Printf("pull message success. nextOffset: %d \n", resp.NextBeginOffset)
			for _, msg := range resp.GetMessageExts() {
				fmt.Printf("pull msg: %v \n", msg)
			}
		}
		offset = resp.NextBeginOffset
	}
}
