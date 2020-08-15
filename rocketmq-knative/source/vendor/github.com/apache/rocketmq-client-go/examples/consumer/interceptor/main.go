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

func main() {
	c, _ := rocketmq.NewPushConsumer(
		consumer.WithGroupName("testGroup"),
		consumer.WithNameServer([]string{"127.0.0.1:9876"}),
		consumer.WithConsumerModel(consumer.Clustering),
		consumer.WithConsumeFromWhere(consumer.ConsumeFromFirstOffset),
		consumer.WithInterceptor(UserFistInterceptor(), UserSecondInterceptor()))
	err := c.Subscribe("TopicTest", consumer.MessageSelector{}, func(ctx context.Context,
		msgs ...*primitive.MessageExt) (consumer.ConsumeResult, error) {
		fmt.Printf("subscribe callback: %v \n", msgs)
		return consumer.ConsumeSuccess, nil
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
		fmt.Printf("Shutdown Consumer error: %s", err.Error())
	}
}

func UserFistInterceptor() primitive.Interceptor {
	return func(ctx context.Context, req, reply interface{}, next primitive.Invoker) error {
		msgCtx, _ := primitive.GetConsumerCtx(ctx)
		fmt.Printf("msgCtx: %v, mehtod: %s", msgCtx, primitive.GetMethod(ctx))

		msgs := req.([]*primitive.MessageExt)
		fmt.Printf("user first interceptor before invoke: %v\n", msgs)
		e := next(ctx, msgs, reply)

		holder := reply.(*consumer.ConsumeResultHolder)
		fmt.Printf("user first interceptor after invoke: %v, result: %v\n", msgs, holder)
		return e
	}
}

func UserSecondInterceptor() primitive.Interceptor {
	return func(ctx context.Context, req, reply interface{}, next primitive.Invoker) error {
		msgs := req.([]*primitive.MessageExt)
		fmt.Printf("user second interceptor before invoke: %v\n", msgs)
		e := next(ctx, msgs, reply)
		holder := reply.(*consumer.ConsumeResultHolder)
		fmt.Printf("user second interceptor after invoke: %v, result: %v\n", msgs, holder)
		return e
	}
}
