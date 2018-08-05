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

package remoting

import (
	"bytes"
	"encoding/binary"
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"math/rand"
	"net"
	"strconv"
	"strings"
	"sync"
	"time"
)

//RemotingClient mq remoting client
type RemotingClient interface {
	//InvokeSync sync invoke remote
	InvokeSync(addr string, request *RemotingCommand, timeoutMillis int64) (remotingCommand *RemotingCommand, err error)
	//InvokeAsync async invoke remote
	InvokeAsync(addr string, request *RemotingCommand, timeoutMillis int64, invokeCallback InvokeCallback) error
	//InvokeOneWay one way invoke remote
	InvokeOneWay(addr string, request *RemotingCommand, timeoutMillis int64) error
}

//DefaultRemotingClient of RemotingClient
type DefaultRemotingClient struct {
	clientId     string
	clientConfig *rocketmqm.MqClientConfig

	connTable     map[string]net.Conn
	connTableLock sync.RWMutex

	responseTable            util.ConcurrentMap //map[int32]*ResponseFuture
	processorTable           util.ConcurrentMap //map[int]ClientRequestProcessor //requestCode|ClientRequestProcessor
	namesrvAddrList          []string
	namesrvAddrSelectedAddr  string
	namesrvAddrSelectedIndex int
	namesvrLockRW            sync.RWMutex
	clientRequestProcessor   ClientRequestProcessor //mange register the processor here
	serializerHandler        SerializerHandler      //rocketmq encode decode
}

//RemotingClientInit create a RemotingClient instance
func RemotingClientInit(clientConfig *rocketmqm.MqClientConfig, clientRequestProcessor ClientRequestProcessor) (client *DefaultRemotingClient) {
	client = &DefaultRemotingClient{}
	client.connTable = map[string]net.Conn{}
	client.responseTable = util.NewConcurrentMap()
	client.clientConfig = clientConfig

	client.namesrvAddrList = strings.Split(clientConfig.NameServerAddress, ";")
	client.namesrvAddrSelectedIndex = -1
	client.clientRequestProcessor = clientRequestProcessor
	client.serializerHandler = newSerializerHandler(clientConfig.ClientSerializeType)
	return
}

//InvokeSync sync invoke remote
func (drc *DefaultRemotingClient) InvokeSync(addr string, request *RemotingCommand, timeoutMillis int64) (remotingCommand *RemotingCommand, err error) {
	var conn net.Conn
	conn, err = drc.getOrCreateConn(addr)
	response := &ResponseFuture{
		SendRequestOK:  false,
		Opaque:         request.Opaque,
		TimeoutMillis:  timeoutMillis,
		BeginTimestamp: time.Now().Unix(),
		Done:           make(chan bool),
	}
	header := drc.serializerHandler.encodeHeader(request)
	body := request.Body
	drc.setResponse(request.Opaque, response)
	err = drc.sendRequest(header, body, conn, addr)
	if err != nil {
		glog.Error(err)
		return
	}
	select {
	case <-response.Done:
		remotingCommand = response.ResponseCommand
		return
	case <-time.After(time.Duration(timeoutMillis) * time.Millisecond):
		err = errors.New("invoke sync timeout:" + strconv.FormatInt(timeoutMillis, 10) + " Millisecond")
		return
	}
}

//InvokeAsync async invoke remote
func (drc *DefaultRemotingClient) InvokeAsync(addr string, request *RemotingCommand, timeoutMillis int64, invokeCallback InvokeCallback) error {
	conn, err := drc.getOrCreateConn(addr)
	if err != nil {
		return err
	}
	response := &ResponseFuture{
		SendRequestOK:  false,
		Opaque:         request.Opaque,
		TimeoutMillis:  timeoutMillis,
		BeginTimestamp: time.Now().Unix(),
		InvokeCallback: invokeCallback,
	}
	drc.setResponse(request.Opaque, response)
	header := drc.serializerHandler.encodeHeader(request)
	body := request.Body
	err = drc.sendRequest(header, body, conn, addr)
	if err != nil {
		glog.Error(err)
		return err
	}
	return err
}

//InvokeOneWay one way invoke remote
func (drc *DefaultRemotingClient) InvokeOneWay(addr string, request *RemotingCommand, timeoutMillis int64) error {
	conn, err := drc.getOrCreateConn(addr)
	if err != nil {
		return err
	}
	header := drc.serializerHandler.encodeHeader(request)
	body := request.Body
	err = drc.sendRequest(header, body, conn, addr)
	if err != nil {
		glog.Error(err)
		return err
	}
	return err
}

func (drc *DefaultRemotingClient) sendRequest(header, body []byte, conn net.Conn, addr string) error {
	var requestBytes []byte
	requestBytes = append(requestBytes, header...)
	if body != nil && len(body) > 0 {
		requestBytes = append(requestBytes, body...)
	}
	_, err := conn.Write(requestBytes)
	if err != nil {
		glog.Error(err)
		if len(addr) > 0 {
			drc.releaseConn(addr, conn)
		}
		return err
	}
	return nil
}

//GetNamesrvAddrList GetNamesrvAddrList
func (drc *DefaultRemotingClient) GetNamesrvAddrList() []string {
	return drc.namesrvAddrList
}

func (drc *DefaultRemotingClient) setResponse(index int32, response *ResponseFuture) {
	drc.responseTable.Set(strconv.Itoa(int(index)), response)
}
func (drc *DefaultRemotingClient) getResponse(index int32) (response *ResponseFuture, err error) {
	obj, ok := drc.responseTable.Get(strconv.Itoa(int(index)))
	if !ok {
		err = errors.New("get conn from responseTable error")
		return
	}
	response = obj.(*ResponseFuture)
	return
}
func (drc *DefaultRemotingClient) removeResponse(index int32) {
	drc.responseTable.Remove(strconv.Itoa(int(index)))
}
func (drc *DefaultRemotingClient) getOrCreateConn(address string) (conn net.Conn, err error) {
	if len(address) == 0 {
		conn, err = drc.getNamesvrConn()
		return
	}
	conn = drc.getConn(address)
	if conn != nil {
		return
	}
	conn, err = drc.createConn(address)
	return
}
func (drc *DefaultRemotingClient) getConn(address string) (conn net.Conn) {
	drc.connTableLock.RLock()
	conn = drc.connTable[address]
	drc.connTableLock.RUnlock()
	return
}
func (drc *DefaultRemotingClient) createConn(address string) (conn net.Conn, err error) {
	defer drc.connTableLock.Unlock()
	drc.connTableLock.Lock()
	conn = drc.connTable[address]
	if conn != nil {
		return
	}
	conn, err = drc.createAndHandleTcpConn(address)
	drc.connTable[address] = conn
	return
}

func (drc *DefaultRemotingClient) getNamesvrConn() (conn net.Conn, err error) {
	drc.namesvrLockRW.RLock()
	address := drc.namesrvAddrSelectedAddr
	drc.namesvrLockRW.RUnlock()
	if len(address) != 0 {
		conn = drc.getConn(address)
		if conn != nil {
			return
		}
	}

	defer drc.namesvrLockRW.Unlock()
	drc.namesvrLockRW.Lock()
	//already connected by another write lock owner
	address = drc.namesrvAddrSelectedAddr
	if len(address) != 0 {
		conn = drc.getConn(address)
		if conn != nil {
			return
		}
	}

	addressCount := len(drc.namesrvAddrList)
	if drc.namesrvAddrSelectedIndex < 0 {
		drc.namesrvAddrSelectedIndex = rand.Intn(addressCount)
	}
	for i := 1; i <= addressCount; i++ {
		selectedIndex := (drc.namesrvAddrSelectedIndex + i) % addressCount
		selectAddress := drc.namesrvAddrList[selectedIndex]
		if len(selectAddress) == 0 {
			continue
		}
		conn, err = drc.createConn(selectAddress)
		if err == nil {
			drc.namesrvAddrSelectedAddr = selectAddress
			drc.namesrvAddrSelectedIndex = selectedIndex
			return
		}
	}
	err = errors.New("all namesvrAddress can't use!,address:" + drc.clientConfig.NameServerAddress)
	return
}
func (drc *DefaultRemotingClient) createAndHandleTcpConn(address string) (conn net.Conn, err error) {
	conn, err = net.Dial("tcp", address)
	if err != nil {
		glog.Error(err)
		return nil, err
	}
	go drc.handlerReceiveLoop(conn, address) //handler连接 处理这个连接返回的结果
	return
}
func (drc *DefaultRemotingClient) releaseConn(addr string, conn net.Conn) {
	defer drc.connTableLock.Unlock()
	conn.Close()
	drc.connTableLock.Lock()
	delete(drc.connTable, addr)
}

func (drc *DefaultRemotingClient) handlerReceiveLoop(conn net.Conn, addr string) (err error) {
	defer func() {
		//when for is break releaseConn
		glog.Error(err, addr)
		drc.releaseConn(addr, conn)
	}()
	b := make([]byte, 1024)
	var length, headerLength, bodyLength int32
	var buf = bytes.NewBuffer([]byte{})
	var header, body []byte
	var readTotalLengthFlag = true //readLen when true,read data when false
	for {
		var n int
		n, err = conn.Read(b)
		if err != nil {
			return
		}
		_, err = buf.Write(b[:n])
		if err != nil {
			return
		}
		for {
			if readTotalLengthFlag {
				//we read 4 bytes of allDataLength
				if buf.Len() >= 4 {
					err = binary.Read(buf, binary.BigEndian, &length)
					if err != nil {
						return
					}
					readTotalLengthFlag = false //now turn to read data
				} else {
					break //wait bytes we not got
				}
			}
			if !readTotalLengthFlag {
				if buf.Len() < int(length) {
					// judge all data received.if not,loop to wait
					break
				}
			}
			//now all data received, we can read totalLen again
			readTotalLengthFlag = true

			//get the data,and handler it
			//header len
			err = binary.Read(buf, binary.BigEndian, &headerLength)
			var realHeaderLen = (headerLength & 0x00ffffff)
			//headerData the first ff is about serializable type
			var headerSerializableType = byte(headerLength >> 24)
			header = make([]byte, realHeaderLen)
			_, err = buf.Read(header)
			bodyLength = length - 4 - realHeaderLen
			body = make([]byte, int(bodyLength))
			if bodyLength == 0 {
				// no body
			} else {
				_, err = buf.Read(body)
			}
			go drc.handlerReceivedMessage(conn, headerSerializableType, header, body)
		}
	}
}
func (drc *DefaultRemotingClient) handlerReceivedMessage(conn net.Conn, headerSerializableType byte, headBytes []byte, bodyBytes []byte) {
	cmd := drc.serializerHandler.decodeRemoteCommand(headerSerializableType, headBytes, bodyBytes)
	if cmd.isResponseType() {
		drc.handlerResponse(cmd)
		return
	}
	go drc.handlerRequest(conn, cmd)
}
func (drc *DefaultRemotingClient) handlerRequest(conn net.Conn, cmd *RemotingCommand) {
	responseCommand := drc.clientRequestProcessor(cmd)
	if responseCommand == nil {
		return
	}
	responseCommand.Opaque = cmd.Opaque
	responseCommand.markResponseType()
	header := drc.serializerHandler.encodeHeader(responseCommand)
	body := responseCommand.Body
	err := drc.sendRequest(header, body, conn, "")
	if err != nil {
		glog.Error(err)
	}
}
func (drc *DefaultRemotingClient) handlerResponse(cmd *RemotingCommand) {
	response, err := drc.getResponse(cmd.Opaque)
	drc.removeResponse(cmd.Opaque)
	if err != nil {
		return
	}
	response.ResponseCommand = cmd
	if response.InvokeCallback != nil {
		response.InvokeCallback(response)
	}

	if response.Done != nil {
		response.Done <- true
	}
}

//ClearExpireResponse clear expire response which is not consumed after 30 seconds
func (drc *DefaultRemotingClient) ClearExpireResponse() {
	for seq, responseObj := range drc.responseTable.Items() {
		response := responseObj.(*ResponseFuture)
		if (response.BeginTimestamp + 30) <= time.Now().Unix() {
			drc.responseTable.Remove(seq)
			if response.InvokeCallback != nil {
				response.InvokeCallback(nil)
				glog.Warningf("remove time out request %v", response)
			}
		}
	}
}
