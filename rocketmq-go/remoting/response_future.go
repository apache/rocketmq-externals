/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package remoting

import (
	"sync"
	"time"
)

type InvokeCallback func(responseFuture *ResponseFuture)

type ResponseFuture struct {
	opaque          int32
	timeoutMillis   time.Duration
	invokeCallback  InvokeCallback
	beginTimestamp  int64
	responseCommand *RemotingCommand
	sendRequestOK   bool
	done            chan bool
	latch           sync.WaitGroup
	err             error
}

func NewResponseFuture(opaque int32, timeout time.Duration, callback InvokeCallback) *ResponseFuture {
	future := &ResponseFuture{
		opaque:         opaque,
		timeoutMillis:  timeout,
		invokeCallback: callback,
		latch:          sync.WaitGroup{},
	}
	future.latch.Add(1)
	return future
}
func (future *ResponseFuture) SetResponseFuture(cmd *RemotingCommand) {
	future.responseCommand = cmd
}

func (future *ResponseFuture) Done() {
	future.latch.Done()
	future.done <- true
}

func (future *ResponseFuture) executeInvokeCallback() {
	future.invokeCallback(nil) // TODO
}

func (future *ResponseFuture) WaitResponse(timeout time.Duration) *RemotingCommand {
	go func() { // TODO optimize
		time.Sleep(timeout)
		future.latch.Add(-1) // TODO whats happened when counter less than 0
	}()
	future.latch.Wait()
	return future.responseCommand
}

func (future *ResponseFuture) String() string {
	return nil
}
