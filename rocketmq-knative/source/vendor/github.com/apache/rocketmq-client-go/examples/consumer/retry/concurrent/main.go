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
	"os"
	"time"

	"github.com/apache/rocketmq-client-go"
	"github.com/apache/rocketmq-client-go/consumer"
	"github.com/apache/rocketmq-client-go/primitive"
)

// use concurrent consumer model, when Subscribe function return consumer.ConsumeRetryLater, the message will be
// send to RocketMQ retry topic. we could set DelayLevelWhenNextConsume in ConsumeConcurrentlyContext, which used to
// indicate the delay of message re-send to origin topic from retry topic.
//
// in this example, we always set DelayLevelWhenNextConsume=1, means that the message will be sent to origin topic after
// 1s. in case of the unlimited retry, we will return consumer.ConsumeSuccess after ReconsumeTimes > 5
func main() {
	c, _ := rocketmq.NewPushConsumer(
		consumer.WithGroupName("testGroup"),
		consumer.WithNameServer([]string{"127.0.0.1:9876"}),
		consumer.WithConsumerModel(consumer.Clustering),
	)

	// The DelayLevel specify the waiting time that before next reconsume,
	// and it range is from 1 to 18 now.
	//
	// The time of each level is the value of indexing of {level-1} in [1s, 5s, 10s, 30s,
	// 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h]
	delayLevel := 1
	err := c.Subscribe("TopicTest", consumer.MessageSelector{}, func(ctx context.Context,
		msgs ...*primitive.MessageExt) (consumer.ConsumeResult, error) {
		fmt.Printf("subscribe callback len: %d \n", len(msgs))

		concurrentCtx, _ := primitive.GetConcurrentlyCtx(ctx)
		concurrentCtx.DelayLevelWhenNextConsume = delayLevel // only run when return consumer.ConsumeRetryLater

		for _, msg := range msgs {
			if msg.ReconsumeTimes > 5 {
				fmt.Printf("msg ReconsumeTimes > 5. msg: %v", msg)
				return consumer.ConsumeSuccess, nil
			} else {
				fmt.Printf("subscribe callback: %v \n", msg)
			}
		}
		return consumer.ConsumeRetryLater, nil
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
