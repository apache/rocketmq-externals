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
package remote

import (
	"bytes"
	"context"
	"errors"
	"math/rand"
	"net"
	"reflect"
	"sync"
	"testing"
	"time"

	"github.com/apache/rocketmq-client-go/internal/utils"

	"github.com/stretchr/testify/assert"
)

func TestNewResponseFuture(t *testing.T) {
	future := NewResponseFuture(context.Background(), 10, nil)
	if future.Opaque != 10 {
		t.Errorf("wrong ResponseFuture's opaque. want=%d, got=%d", 10, future.Opaque)
	}
	if future.Err != nil {
		t.Errorf("wrong RespnseFuture's Err. want=<nil>, got=%v", future.Err)
	}
	if future.callback != nil {
		t.Errorf("wrong ResponseFuture's callback. want=<nil>, got!=<nil>")
	}
	if future.Done == nil {
		t.Errorf("wrong ResponseFuture's done. want=<channel>, got=<nil>")
	}
}

func TestResponseFutureTimeout(t *testing.T) {
	callback := func(r *ResponseFuture) {
		if r.ResponseCommand.Remark == "" {
			r.ResponseCommand.Remark = "Hello RocketMQ."
		} else {
			r.ResponseCommand.Remark = r.ResponseCommand.Remark + "Go Client"
		}
	}
	future := NewResponseFuture(context.Background(), 10, callback)
	future.ResponseCommand = NewRemotingCommand(200,
		nil, nil)

	var wg sync.WaitGroup
	wg.Add(10)
	for i := 0; i < 10; i++ {
		go func() {
			future.executeInvokeCallback()
			wg.Done()
		}()
	}
	wg.Wait()
	if future.ResponseCommand.Remark != "Hello RocketMQ." {
		t.Errorf("wrong ResponseFuture.ResponseCommand.Remark. want=%s, got=%s",
			"Hello RocketMQ.", future.ResponseCommand.Remark)
	}

}

func TestResponseFutureWaitResponse(t *testing.T) {
	ctx, _ := context.WithTimeout(context.Background(), time.Duration(1000))
	future := NewResponseFuture(ctx, 10, nil)
	if _, err := future.waitResponse(); err != utils.ErrRequestTimeout {
		t.Errorf("wrong ResponseFuture waitResponse. want=%v, got=%v",
			utils.ErrRequestTimeout, err)
	}
	future = NewResponseFuture(context.Background(), 10, nil)
	responseError := errors.New("response error")
	go func() {
		time.Sleep(100 * time.Millisecond)
		future.Err = responseError
		future.Done <- true
	}()
	if _, err := future.waitResponse(); err != responseError {
		t.Errorf("wrong ResponseFuture waitResponse. want=%v. got=%v",
			responseError, err)
	}
	future = NewResponseFuture(context.Background(), 10, nil)
	responseRemotingCommand := NewRemotingCommand(202, nil, nil)
	go func() {
		time.Sleep(100 * time.Millisecond)
		future.ResponseCommand = responseRemotingCommand
		future.Done <- true
	}()
	if r, err := future.waitResponse(); err != nil {
		t.Errorf("wrong ResponseFuture waitResponse error: %v", err)
	} else {
		if r != responseRemotingCommand {
			t.Errorf("wrong ResponseFuture waitResposne result. want=%v, got=%v",
				responseRemotingCommand, r)
		}
	}
}

func TestCreateScanner(t *testing.T) {
	r := randomNewRemotingCommand()
	content, err := encode(r)
	if err != nil {
		t.Fatalf("failed to encode RemotingCommand. %s", err)
	}
	client := NewRemotingClient()
	reader := bytes.NewReader(content)
	scanner := client.createScanner(reader)
	for scanner.Scan() {
		rcr, err := decode(scanner.Bytes())
		if err != nil {
			t.Fatalf("failedd to decode RemotingCommand from scanner")
		}
		if r.Code != rcr.Code {
			t.Fatalf("wrong Code. want=%d, got=%d", r.Code, rcr.Code)
		}
		if r.Version != rcr.Version {
			t.Fatalf("wrong Version. want=%d, got=%d", r.Version, rcr.Version)
		}
		if r.Opaque != rcr.Opaque {
			t.Fatalf("wrong opaque. want=%d, got=%d", r.Opaque, rcr.Opaque)
		}
		if r.Flag != rcr.Flag {
			t.Fatalf("wrong flag. want=%d, got=%d", r.Opaque, rcr.Opaque)
		}
		if !reflect.DeepEqual(r.ExtFields, rcr.ExtFields) {
			t.Fatalf("wrong extFields. want=%v, got=%v", r.ExtFields, rcr.ExtFields)
		}
	}
}

func TestInvokeSync(t *testing.T) {
	addr := ":3004"

	clientSendRemtingCommand := NewRemotingCommand(10, nil, []byte("Hello RocketMQ"))
	serverSendRemotingCommand := NewRemotingCommand(20, nil, []byte("Welcome native"))
	serverSendRemotingCommand.Opaque = clientSendRemtingCommand.Opaque
	serverSendRemotingCommand.Flag = ResponseType
	var wg sync.WaitGroup
	wg.Add(1)
	client := NewRemotingClient()

	var clientSend sync.WaitGroup // blocking client send message until the server listen success.
	clientSend.Add(1)

	go func() {
		clientSend.Wait()
		receiveCommand, err := client.InvokeSync(context.Background(), addr,
			clientSendRemtingCommand)
		if err != nil {
			t.Fatalf("failed to invoke synchronous. %s", err)
		} else {
			assert.Equal(t, len(receiveCommand.ExtFields), 0)
			assert.Equal(t, len(serverSendRemotingCommand.ExtFields), 0)
			// in order to avoid the difference of ExtFields between the receiveCommand and serverSendRemotingCommand
			// the ExtFields in receiveCommand is map[string]string(nil), but serverSendRemotingCommand is map[string]string{}
			receiveCommand.ExtFields = nil
			serverSendRemotingCommand.ExtFields = nil
			assert.Equal(t, receiveCommand, serverSendRemotingCommand, "remotingCommand prased in client is different from server.")
		}
		wg.Done()
	}()

	l, err := net.Listen("tcp", addr)
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	clientSend.Done()
	for {
		conn, err := l.Accept()
		if err != nil {
			return
		}
		defer conn.Close()
		scanner := client.createScanner(conn)
		for scanner.Scan() {
			receivedRemotingCommand, err := decode(scanner.Bytes())
			if err != nil {
				t.Errorf("failed to decode RemotingCommnad. %s", err)
			}
			if clientSendRemtingCommand.Code != receivedRemotingCommand.Code {
				t.Errorf("wrong code. want=%d, got=%d", receivedRemotingCommand.Code,
					clientSendRemtingCommand.Code)
			}
			body, err := encode(serverSendRemotingCommand)
			if err != nil {
				t.Fatalf("failed to encode RemotingCommand")
			}
			_, err = conn.Write(body)
			if err != nil {
				t.Fatalf("failed to write body to conneciton.")
			}
			goto done
		}
	}
done:
	wg.Wait()
}

func TestInvokeAsync(t *testing.T) {
	addr := ":3006"
	var wg sync.WaitGroup
	cnt := 50
	wg.Add(cnt)
	client := NewRemotingClient()
	for i := 0; i < cnt; i++ {
		go func(index int) {
			time.Sleep(time.Duration(rand.Intn(100)) * time.Millisecond)
			t.Logf("[Send: %d] asychronous message", index)
			sendRemotingCommand := randomNewRemotingCommand()
			err := client.InvokeAsync(context.Background(), addr, sendRemotingCommand, func(r *ResponseFuture) {
				t.Logf("[Receive: %d] asychronous message response", index)
				if string(sendRemotingCommand.Body) != string(r.ResponseCommand.Body) {
					t.Errorf("wrong response message. want=%s, got=%s", string(sendRemotingCommand.Body),
						string(r.ResponseCommand.Body))
				}
				wg.Done()
			})
			if err != nil {
				t.Errorf("failed to invokeAsync. %s", err)
			}

		}(i)
	}
	l, err := net.Listen("tcp", addr)
	if err != nil {
		t.Fatalf("failed to create tcp network. %s", err)
	}
	defer l.Close()
	count := 0
	for {
		conn, err := l.Accept()
		if err != nil {
			t.Fatalf("failed to create connection. %s", err)
		}
		defer conn.Close()
		scanner := client.createScanner(conn)
		for scanner.Scan() {
			t.Log("receive request")
			r, err := decode(scanner.Bytes())
			if err != nil {
				t.Errorf("failed to decode RemotingCommand %s", err)
			}
			r.markResponseType()
			body, _ := encode(r)
			_, err = conn.Write(body)
			if err != nil {
				t.Fatalf("failed to send response %s", err)
			}
			count++
			if count >= cnt {
				goto done
			}
		}
	}
done:

	wg.Wait()
}

func TestInvokeAsyncTimeout(t *testing.T) {
	addr := ":3002"

	clientSendRemtingCommand := NewRemotingCommand(10, nil, []byte("Hello RocketMQ"))
	serverSendRemotingCommand := NewRemotingCommand(20, nil, []byte("Welcome native"))
	serverSendRemotingCommand.Opaque = clientSendRemtingCommand.Opaque
	serverSendRemotingCommand.Flag = ResponseType

	var wg sync.WaitGroup
	wg.Add(1)
	client := NewRemotingClient()

	var clientSend sync.WaitGroup // blocking client send message until the server listen success.
	clientSend.Add(1)
	go func() {
		clientSend.Wait()
		ctx, _ := context.WithTimeout(context.Background(), time.Duration(10*time.Second))
		err := client.InvokeAsync(ctx, addr, clientSendRemtingCommand,
			func(r *ResponseFuture) {
				assert.NotNil(t, r.Err)
				assert.Equal(t, utils.ErrRequestTimeout, r.Err)
				wg.Done()
			})
		assert.Nil(t, err, "failed to invokeSync.")
	}()

	l, err := net.Listen("tcp", addr)
	assert.Nil(t, err)
	defer l.Close()
	clientSend.Done()

	for {
		conn, err := l.Accept()
		assert.Nil(t, err)
		defer conn.Close()

		scanner := client.createScanner(conn)
		for scanner.Scan() {
			t.Logf("receive request.")
			_, err := decode(scanner.Bytes())
			assert.Nil(t, err, "failed to decode RemotingCommnad.")

			time.Sleep(5 * time.Second) // force client timeout
			goto done
		}
	}
done:
	wg.Wait()
}

func TestInvokeOneWay(t *testing.T) {
	addr := ":3008"
	clientSendRemtingCommand := NewRemotingCommand(10, nil, []byte("Hello RocketMQ"))

	var wg sync.WaitGroup
	wg.Add(1)
	client := NewRemotingClient()

	var clientSend sync.WaitGroup // blocking client send message until the server listen success.
	clientSend.Add(1)
	go func() {
		clientSend.Wait()
		err := client.InvokeOneWay(context.Background(), addr, clientSendRemtingCommand)
		if err != nil {
			t.Fatalf("failed to invoke synchronous. %s", err)
		}
		wg.Done()
	}()

	l, err := net.Listen("tcp", addr)
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	clientSend.Done()
	for {
		conn, err := l.Accept()
		if err != nil {
			return
		}
		defer conn.Close()
		scanner := client.createScanner(conn)
		for scanner.Scan() {
			receivedRemotingCommand, err := decode(scanner.Bytes())
			if err != nil {
				t.Errorf("failed to decode RemotingCommnad. %s", err)
			}
			if clientSendRemtingCommand.Code != receivedRemotingCommand.Code {
				t.Errorf("wrong code. want=%d, got=%d", receivedRemotingCommand.Code,
					clientSendRemtingCommand.Code)
			}
			goto done
		}
	}
done:
	wg.Wait()
}
