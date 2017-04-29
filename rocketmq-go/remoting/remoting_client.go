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
	"errors"
	"fmt"
	"github.com/golang/glog"
	"math/rand"
	"net"
	"sync"
	"time"
)

type ConnEventListener interface {
	OnConnConnect(remoteAddress string, conn net.Conn)
	OnConnClose(remoteAddress string, conn net.Conn)
	OnConnIdle(remoteAddress string, conn net.Conn)
	OnConnException(remoteAddress string, conn net.Conn)
}

type NetRequestProcessor struct {
}

type NetConfig struct {
	clientWorkerNumber           int
	clientCallbackExecutorNumber int
	clientOneWaySemaphoreValue   int
	clientAsyncSemaphoreValue    int
	connectTimeoutMillis         time.Duration
	channelNotActiveInterval     time.Duration

	clientChannelMaxIdleTimeSeconds    time.Duration
	clientSocketSndBufSize             int
	clientSocketRcvBufSize             int
	clientPooledByteBufAllocatorEnable bool
	clientCloseSocketIfTimeout         bool
}

type Pair struct {
	o1 *NetRequestProcessor
	o2 *ExecutorService
}

type RemotingClient struct {
	semaphoreOneWay         sync.Mutex // TODO right? use chan?
	semaphoreAsync          sync.Mutex
	processorTable          map[int]*Pair
	netEventExecutor        *NetEventExecutor
	defaultRequestProcessor *Pair

	config             NetConfig
	connTable          map[string]net.Conn
	connTableLock      sync.RWMutex
	timer              *time.Timer
	namesrvAddrList    []string
	namesrvAddrChoosed string

	callBackExecutor  *ExecutorService
	listener          ConnEventListener
	rpcHook           RPCHook
	responseTable     map[int32]*ResponseFuture
	responseTableLock sync.RWMutex
}

type ConnHandlerContext struct {
}

type ExecutorService struct {
	callBackChannel chan func()
	quit            chan bool
}

func (exec *ExecutorService) submit(callback func()) {
	exec.callBackChannel <- callback
}

func (exec *ExecutorService) run() {
	go func() {
		glog.Info("Callback Executor routing start.")
		for {
			select {
			case invoke := <-exec.callBackChannel:
				invoke()
			case <-exec.quit:
				return
			}
		}
		glog.Info("Callback Executor routing quit.")
	}()
}

func NewRemotingClient(cfg NetConfig) *RemotingClient {
	client := &RemotingClient{
		config:        cfg,
		connTable:     make(map[string]net.Conn),
		timer:         time.NewTimer(10 * time.Second),
		responseTable: make(map[int32]*ResponseFuture),
	}
	// java: super(xxxx)
	return client
}

func initValueIndex() int {
	r := rand.Int()
	if r < 0 { // math.Abs para is float64
		r = -r
	}
	return r % 999 % 999
}

func (rc *RemotingClient) Start() {
	// TODO
}

func (rc *RemotingClient) Shutdown() {
	// TODO
	rc.timer.Stop()
}

func (rc *RemotingClient) registerRPCHook(hk RPCHook) {
	rc.rpcHook = hk
}

func (rc *RemotingClient) CloseConn(addr string) {
	// TODO
}

// check timeout future
func (rc *RemotingClient) updateNameServerAddressList(addrs []string) {
	old, update := rc.namesrvAddrList, false

	if addrs != nil && len(addrs) > 0 {
		if old == nil || len(addrs) != len(old) {
			update = true
		} else {
			for i := 0; i < len(addrs) && !update; i++ {
				if contains(old, addrs[i]) {
					update = true
				}
			}
		}
	}

	if update {
		rc.namesrvAddrList = addrs // TODO safe?
	}

}

func (rc *RemotingClient) invokeSync(addr string, request *RemotingCommand,
	timeout time.Duration) (*RemotingCommand, error) {
	conn := rc.getAndCreateConn(addr)
	if conn != nil {
		if rc.rpcHook != nil {
			rc.rpcHook.DoBeforeRequest(addr, request)
		}
		opaque := request.opaque
		//defer delete(rc.responseTable, opaque) TODO should in listener

		future := &ResponseFuture{
			opaque:        opaque,
			timeoutMillis: timeout,
		}
		rc.responseTable[opaque] = future

		conn.Write(request.encode()) // TODO register listener

		response := future.WaitResponse(timeout)
		if response == nil {
			if future.sendRequestOK {
				return nil, errors.New(fmt.Sprintf("RemotingTimeout error: %s", future.err.Error()))
			} else {
				return nil, errors.New(fmt.Sprintf("RemotingSend error: %s", future.err.Error()))
			}
		}

		if rc.rpcHook != nil {
			rc.rpcHook.DoBeforeResponse(addr, response)
		}
		return response, nil
	} else {
		rc.CloseConn(addr) // TODO
		return nil, errors.New(fmt.Sprintf("Connection to %s ERROR!", addr))
	}
}

func (rc *RemotingClient) PutNetEvent(event *NetEvent) {
	rc.netEventExecutor.PutEvent(event)
}

func (rc *RemotingClient) executeInvokeCallback(future *ResponseFuture) {
	executor := rc.CallbackExecutor()
	if executor != nil {
		executor.submit(func() {
			future.invokeCallback(future)
		})
		return
	}
	future.executeInvokeCallback()
}

func (rc *RemotingClient) scanResponseTable() {
	rfMap := make(map[int]*ResponseFuture)
	for k, future := range rc.responseTable { // TODO safety?
		if int64(future.beginTimestamp)+int64(future.timeoutMillis)+1e9 <= time.Now().Unix() {
			future.Done()
			delete(rc.responseTable, k)
			rfMap[int(k)] = future
			glog.Warningf("remove timeout request, ", future.String())
		}
	}

	go func() {
		for _, future := range rfMap {
			rc.executeInvokeCallback(future) // TODO if still failed, how to deal with the message ?
		}
	}()
}

func (rc *RemotingClient) CallbackExecutor() *ExecutorService {
	return rc.callBackExecutor
}

func (rc *RemotingClient) RPCHook() RPCHook {
	return rc.rpcHook
}

func (rc *RemotingClient) ConnEventListener() ConnEventListener {
	return rc.listener
}

func (rc *RemotingClient) invokeAsync(addr string, request *RemotingCommand,
	timeout time.Duration, callback InvokeCallback) error {
	conn := rc.getAndCreateConn(addr)
	if conn != nil { // TODO how to confirm conn active?
		if rc.rpcHook != nil {
			rc.rpcHook.DoBeforeRequest(addr, request)
		}

		opaque := request.opaque
		acquired := false // TODO semaphore.tryAcquire...
		if acquired {
			//final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);

			future := NewResponseFuture(opaque, timeout, callback)
			rc.responseTable[opaque] = future

			future.WaitResponse(timeout) // TODO add listener
		}
		return nil
	} else {
		rc.CloseConn(addr) // TODO
		return errors.New(fmt.Sprintf("Connection to %s ERROR!", addr))
	}
}

func (rc *RemotingClient) invokeOneWay(addr string, request *RemotingCommand,
	timeout time.Duration) error {
	conn := rc.getAndCreateConn(addr)
	if conn != nil {
		if rc.rpcHook != nil {
			rc.rpcHook.DoBeforeRequest(addr, request)
		}

		request.MarkOneWayRpc()
		// TODO boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
		return nil
	} else {
		rc.CloseConn(addr) // TODO
		return errors.New(fmt.Sprintf("Connection to %s ERROR!", addr))
	}
}

func (rc *RemotingClient) getAndCreateConn(addr string) net.Conn {
	return nil
}

func (rc *RemotingClient) getAndCreateNamesrvConn() net.Conn {
	return nil
}

func (rc *RemotingClient) createConn(addr string) net.Conn {
	return nil
}

func (rc *RemotingClient) RegisterProcessor(requestCode int, processor *NetRequestProcessor, executor ExecutorService) {
	// TODO
}

func (rc *RemotingClient) ConnWriteable(address string) bool {
	return false
}

func (rc *RemotingClient) NameServerAddressList() []string {
	return nil
}

func (rc *RemotingClient) String() string {
	return nil // TODO
}

func contains(s []string, o string) bool { // TODO optimize
	for _, v := range s {
		if o == v {
			return true
		}
	}
	return false
}
