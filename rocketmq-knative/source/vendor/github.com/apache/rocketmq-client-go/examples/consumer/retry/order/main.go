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

/**
 * use orderly consumer model, when Subscribe function return consumer.SuspendCurrentQueueAMoment, it will be re-send to
 * local msg queue for later consume if msg.ReconsumeTimes < MaxReconsumeTimes, otherwise, it will be send to rocketmq
 * DLQ topic, we should manually resolve the msg.
 */
package main

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/apache/rocketmq-client-go"
	"github.com/apache/rocketmq-client-go/consumer"
	"github.com/apache/rocketmq-client-go/primitive"
)

func main() {
	c, _ := rocketmq.NewPushConsumer(
		consumer.WithGroupName("testGroup"),
		consumer.WithNameServer([]string{"127.0.0.1:9876"}),
		consumer.WithConsumerModel(consumer.Clustering),
		consumer.WithConsumeFromWhere(consumer.ConsumeFromFirstOffset),
		consumer.WithConsumerOrder(true),
		consumer.WithMaxReconsumeTimes(5),
	)

	err := c.Subscribe("TopicTest", consumer.MessageSelector{}, func(ctx context.Context,
		msgs ...*primitive.MessageExt) (consumer.ConsumeResult, error) {
		orderlyCtx, _ := primitive.GetOrderlyCtx(ctx)
		fmt.Printf("orderly context: %v\n", orderlyCtx)
		fmt.Printf("subscribe orderly callback len: %d \n", len(msgs))

		for _, msg := range msgs {
			if msg.ReconsumeTimes > 5 {
				fmt.Printf("msg ReconsumeTimes > 5. msg: %v", msg)
			} else {
				fmt.Printf("subscribe orderly callback: %v \n", msg)
			}
		}
		return consumer.SuspendCurrentQueueAMoment, nil

	})
	if err != nil {
		fmt.Println(err.Error())
	}
	// Note: start after subscribe
	err = c.Start()
	if err != nil {
		fmt.Println(err.Error())
		os.Exit(-1)
	}
	time.Sleep(time.Hour)
	err = c.Shutdown()
	if err != nil {
		fmt.Printf("shundown Consumer error: %s", err.Error())
	}
}
