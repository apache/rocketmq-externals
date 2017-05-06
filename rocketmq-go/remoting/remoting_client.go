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
	"bytes"
	"encoding/binary"
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
	"math/rand"
	"net"
	"strconv"
	"strings"
	"sync"
	"time"
)

type RemotingClient interface {
	InvokeSync(addr string, request *RemotingCommand, timeoutMillis int64) (remotingCommand *RemotingCommand, err error)
	InvokeAsync(addr string, request *RemotingCommand, timeoutMillis int64, invokeCallback InvokeCallback) error
	InvokeOneWay(addr string, request *RemotingCommand, timeoutMillis int64) error
}
type DefalutRemotingClient struct {
	clientId     string
	clientConfig *config.ClientConfig

	connTable     map[string]net.Conn
	connTableLock sync.RWMutex

	responseTable  util.ConcurrentMap //map[int32]*ResponseFuture
	processorTable util.ConcurrentMap //map[int]ClientRequestProcessor //requestCode|ClientRequestProcessor
	//	protected final HashMap<Integer/* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
	//new HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>>(64);
	namesrvAddrList          []string
	namesrvAddrSelectedAddr  string
	namesrvAddrSelectedIndex int                    //how to chose. done
	namesvrLockRW            sync.RWMutex           //
	clientRequestProcessor   ClientRequestProcessor //mange register the processor here
	serializerHandler        SerializerHandler      //rocketmq encode decode
}

func RemotingClientInit(clientConfig *config.ClientConfig, clientRequestProcessor ClientRequestProcessor) (client *DefalutRemotingClient) {
	client = &DefalutRemotingClient{}
	client.connTable = map[string]net.Conn{}
	client.responseTable = util.New()
	client.clientConfig = clientConfig

	client.namesrvAddrList = strings.Split(clientConfig.NameServerAddress(), ";")
	client.namesrvAddrSelectedIndex = -1
	client.clientRequestProcessor = clientRequestProcessor
	client.serializerHandler = NewSerializerHandler()
	return
}

func (self *DefalutRemotingClient) InvokeSync(addr string, request *RemotingCommand, timeoutMillis int64) (remotingCommand *RemotingCommand, err error) {
	var conn net.Conn
	conn, err = self.GetOrCreateConn(addr)
	response := &ResponseFuture{
		SendRequestOK:  false,
		Opaque:         request.Opaque,
		TimeoutMillis:  timeoutMillis,
		BeginTimestamp: time.Now().Unix(),
		Done:           make(chan bool),
	}
	header := self.serializerHandler.EncodeHeader(request)
	body := request.Body
	self.SetResponse(request.Opaque, response)
	err = self.sendRequest(header, body, conn, addr)
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
func (self *DefalutRemotingClient) InvokeAsync(addr string, request *RemotingCommand, timeoutMillis int64, invokeCallback InvokeCallback) error {
	conn, err := self.GetOrCreateConn(addr)
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
	self.SetResponse(request.Opaque, response)
	header := self.serializerHandler.EncodeHeader(request)
	body := request.Body
	err = self.sendRequest(header, body, conn, addr)
	if err != nil {
		glog.Error(err)
		return err
	}
	return err
}
func (self *DefalutRemotingClient) InvokeOneWay(addr string, request *RemotingCommand, timeoutMillis int64) error {
	conn, err := self.GetOrCreateConn(addr)
	if err != nil {
		return err
	}
	header := self.serializerHandler.EncodeHeader(request)
	body := request.Body
	err = self.sendRequest(header, body, conn, addr)
	if err != nil {
		glog.Error(err)
		return err
	}
	return err
}

func (self *DefalutRemotingClient) sendRequest(header, body []byte, conn net.Conn, addr string) error {
	var requestBytes []byte
	requestBytes = append(requestBytes, header...)
	if body != nil && len(body) > 0 {
		requestBytes = append(requestBytes, body...)
	}
	_, err := conn.Write(requestBytes)
	if err != nil {
		glog.Error(err)
		if len(addr) > 0 {
			self.ReleaseConn(addr, conn)
		}
		return err
	}
	return nil
}
func (self *DefalutRemotingClient) GetNamesrvAddrList() []string {
	return self.namesrvAddrList
}

func (self *DefalutRemotingClient) SetResponse(index int32, response *ResponseFuture) {
	self.responseTable.Set(strconv.Itoa(int(index)), response)
}
func (self *DefalutRemotingClient) getResponse(index int32) (response *ResponseFuture, err error) {
	obj, ok := self.responseTable.Get(strconv.Itoa(int(index)))
	if !ok {
		err = errors.New("get conn from responseTable error")
		return
	}
	response = obj.(*ResponseFuture)
	return
}
func (self *DefalutRemotingClient) removeResponse(index int32) {
	self.responseTable.Remove(strconv.Itoa(int(index)))
}
func (self *DefalutRemotingClient) GetOrCreateConn(address string) (conn net.Conn, err error) {
	if len(address) == 0 {
		conn, err = self.getNamesvrConn()
		return
	}
	conn = self.GetConn(address)
	if conn != nil {
		return
	}
	conn, err = self.CreateConn(address)
	return
}
func (self *DefalutRemotingClient) GetConn(address string) (conn net.Conn) {
	self.connTableLock.RLock()
	conn = self.connTable[address]
	self.connTableLock.RUnlock()
	return
}
func (self *DefalutRemotingClient) CreateConn(address string) (conn net.Conn, err error) {
	defer self.connTableLock.Unlock()
	self.connTableLock.Lock()
	conn = self.connTable[address]
	if conn != nil {
		return
	}
	conn, err = self.createAndHandleTcpConn(address)
	self.connTable[address] = conn
	return
}

func (self *DefalutRemotingClient) getNamesvrConn() (conn net.Conn, err error) {
	self.namesvrLockRW.RLock()
	address := self.namesrvAddrSelectedAddr
	self.namesvrLockRW.RUnlock()
	if len(address) != 0 {
		conn = self.GetConn(address)
		if conn != nil {
			return
		}
	}

	defer self.namesvrLockRW.Unlock()
	self.namesvrLockRW.Lock()
	//already connected by another write lock owner
	address = self.namesrvAddrSelectedAddr
	if len(address) != 0 {
		conn = self.GetConn(address)
		if conn != nil {
			return
		}
	}

	addressCount := len(self.namesrvAddrList)
	if self.namesrvAddrSelectedIndex < 0 {
		self.namesrvAddrSelectedIndex = rand.Intn(addressCount)
	}
	for i := 1; i <= addressCount; i++ {
		selectedIndex := (self.namesrvAddrSelectedIndex + i) % addressCount
		selectAddress := self.namesrvAddrList[selectedIndex]
		if len(selectAddress) == 0 {
			continue
		}
		conn, err = self.CreateConn(selectAddress)
		if err == nil {
			self.namesrvAddrSelectedAddr = selectAddress
			self.namesrvAddrSelectedIndex = selectedIndex
			return
		}
	}
	err = errors.New("all namesvrAddress can't use!,address:" + self.clientConfig.NameServerAddress())
	return
}
func (self *DefalutRemotingClient) createAndHandleTcpConn(address string) (conn net.Conn, err error) {
	conn, err = net.Dial("tcp", address)
	if err != nil {
		glog.Error(err)
		return nil, err
	}
	go self.handlerReceiveLoop(conn, address) //handler连接 处理这个连接返回的结果
	return
}
func (self *DefalutRemotingClient) ReleaseConn(addr string, conn net.Conn) {
	defer self.connTableLock.Unlock()
	conn.Close()
	self.connTableLock.Lock()
	delete(self.connTable, addr)
}

func (self *DefalutRemotingClient) handlerReceiveLoop(conn net.Conn, addr string) (err error) {
	defer func() {
		//when for is break releaseConn
		glog.Error(err, addr)
		self.ReleaseConn(addr, conn)
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
			go self.handlerReceivedMessage(conn, headerSerializableType, header, body)
		}
	}
}
func (self *DefalutRemotingClient) handlerReceivedMessage(conn net.Conn, headerSerializableType byte, headBytes []byte, bodyBytes []byte) {
	cmd := self.serializerHandler.DecodeRemoteCommand(headerSerializableType, headBytes, bodyBytes)
	if cmd.IsResponseType() {
		self.handlerResponse(cmd)
		return
	}
	go self.handlerRequest(conn, cmd)
}
func (self *DefalutRemotingClient) handlerRequest(conn net.Conn, cmd *RemotingCommand) {
	responseCommand := self.clientRequestProcessor(cmd)
	if responseCommand == nil {
		return
	}
	responseCommand.Opaque = cmd.Opaque
	responseCommand.MarkResponseType()
	header := self.serializerHandler.EncodeHeader(responseCommand)
	body := responseCommand.Body
	err := self.sendRequest(header, body, conn, "")
	if err != nil {
		glog.Error(err)
	}
}
func (self *DefalutRemotingClient) handlerResponse(cmd *RemotingCommand) {
	response, err := self.getResponse(cmd.Opaque)
	self.removeResponse(cmd.Opaque)
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

func (self *DefalutRemotingClient) ClearExpireResponse() {
	for seq, responseObj := range self.responseTable.Items() {
		response := responseObj.(*ResponseFuture)
		if (response.BeginTimestamp + 30) <= time.Now().Unix() {
			//30 mins expire
			self.responseTable.Remove(seq)
			if response.InvokeCallback != nil {
				response.InvokeCallback(nil)
				glog.Warningf("remove time out request %v", response)
			}
		}
	}
}
