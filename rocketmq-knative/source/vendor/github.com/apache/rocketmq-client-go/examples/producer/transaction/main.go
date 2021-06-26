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
	"strconv"
	"sync"
	"sync/atomic"
	"time"

	"github.com/apache/rocketmq-client-go"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/producer"
)

type DemoListener struct {
	localTrans       *sync.Map
	transactionIndex int32
}

func NewDemoListener() *DemoListener {
	return &DemoListener{
		localTrans: new(sync.Map),
	}
}

func (dl *DemoListener) ExecuteLocalTransaction(msg *primitive.Message) primitive.LocalTransactionState {
	nextIndex := atomic.AddInt32(&dl.transactionIndex, 1)
	fmt.Printf("nextIndex: %v for transactionID: %v\n", nextIndex, msg.TransactionId)
	status := nextIndex % 3
	dl.localTrans.Store(msg.TransactionId, primitive.LocalTransactionState(status+1))

	fmt.Printf("dl")
	return primitive.UnknowState
}

func (dl *DemoListener) CheckLocalTransaction(msg *primitive.MessageExt) primitive.LocalTransactionState {
	fmt.Printf("%v msg transactionID : %v\n", time.Now(), msg.TransactionId)
	v, existed := dl.localTrans.Load(msg.TransactionId)
	if !existed {
		fmt.Printf("unknow msg: %v, return Commit", msg)
		return primitive.CommitMessageState
	}
	state := v.(primitive.LocalTransactionState)
	switch state {
	case 1:
		fmt.Printf("checkLocalTransaction COMMIT_MESSAGE: %v\n", msg)
		return primitive.CommitMessageState
	case 2:
		fmt.Printf("checkLocalTransaction ROLLBACK_MESSAGE: %v\n", msg)
		return primitive.RollbackMessageState
	case 3:
		fmt.Printf("checkLocalTransaction unknow: %v\n", msg)
		return primitive.UnknowState
	default:
		fmt.Printf("checkLocalTransaction default COMMIT_MESSAGE: %v\n", msg)
		return primitive.CommitMessageState
	}
}

func main() {
	p, _ := rocketmq.NewTransactionProducer(
		NewDemoListener(),
		producer.WithNameServer([]string{"127.0.0.1:9876"}),
		producer.WithRetry(1),
	)
	err := p.Start()
	if err != nil {
		fmt.Printf("start producer error: %s\n", err.Error())
		os.Exit(1)
	}

	for i := 0; i < 10; i++ {
		res, err := p.SendMessageInTransaction(context.Background(),
			primitive.NewMessage("TopicTest5", []byte("Hello RocketMQ again "+strconv.Itoa(i))))

		if err != nil {
			fmt.Printf("send message error: %s\n", err)
		} else {
			fmt.Printf("send message success: result=%s\n", res.String())
		}
	}
	time.Sleep(5 * time.Minute)
	err = p.Shutdown()
	if err != nil {
		fmt.Printf("shutdown producer error: %s", err.Error())
	}
}
