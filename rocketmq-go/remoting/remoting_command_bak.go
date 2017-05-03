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

// TODO: refactor
import (
	//"bytes"
	//"encoding/binary"
	//"encoding/json"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/header"
	//"log"
	//"os"
	//"strconv"
	//"sync"
	//"sync/atomic"
)

func init() {
	// TODO
}

//const (
//	SerializeTypeProperty = "rocketmq.serialize.type"
//	SerializeTypeEnv      = "ROCKETMQ_SERIALIZE_TYPE"
//	RemotingVersionKey    = "rocketmq.remoting.version"
//	rpcType               = 0 // 0, request command
//	rpcOneWay             = 1 // 0, RPC
//)
//
//type RemotingCommandType int
//
//const (
//	ResponseCommand RemotingCommandType = iota
//	RqeusetCommand
//)
//
//var configVersion int = -1
//var requestId int32
//var decodeLock sync.Mutex
//
//type RemotingCommand struct {
//	//header
//	code      int               `json:"code"`
//	language  string            `json:"language"`
//	version   int               `json:"version"`
//	opaque    int32             `json:"opaque"`
//	flag      int               `json:"flag"`
//	remark    string            `json:"remark"`
//	extFields map[string]string `json:"extFields"`
//	header    CustomerHeader    // transient
//	//body
//	body []byte `json:"body,omitempty"`
//}
//
//func NewRemotingCommand(code int, header CustomerHeader) *RemotingCommand {
//	cmd := &RemotingCommand{
//		code:   code,
//		header: header,
//	}
//	setCmdVersion(cmd)
//	return cmd
//}
//
//func setCmdVersion(cmd *RemotingCommand) {
//	if configVersion >= 0 {
//		cmd.version = configVersion // safety
//	} else if v := os.Getenv(RemotingVersionKey); v != "" {
//		value, err := strconv.Atoi(v)
//		if err != nil {
//			// TODO log
//		}
//		cmd.version = value
//		configVersion = value
//	}
//}
//
//func (cmd *RemotingCommand) encodeHeader() []byte {
//	length := 4
//	headerData := cmd.buildHeader()
//	length += len(headerData)
//
//	if cmd.body != nil {
//		length += len(cmd.body)
//	}
//
//	buf := bytes.NewBuffer([]byte{})
//	binary.Write(buf, binary.BigEndian, length)
//	binary.Write(buf, binary.BigEndian, len(cmd.body))
//	buf.Write(headerData)
//
//	return buf.Bytes()
//}
//
//func (cmd *RemotingCommand) buildHeader() []byte {
//	buf, err := json.Marshal(cmd)
//	if err != nil {
//		return nil
//	}
//	return buf
//}
//
//func (cmd *RemotingCommand) encode() []byte {
//	length := 4
//
//	headerData := cmd.buildHeader()
//	length += len(headerData)
//
//	if cmd.body != nil {
//		length += len(cmd.body)
//	}
//
//	buf := bytes.NewBuffer([]byte{})
//	binary.Write(buf, binary.LittleEndian, length)
//	binary.Write(buf, binary.LittleEndian, len(cmd.body))
//	buf.Write(headerData)
//
//	if cmd.body != nil {
//		buf.Write(cmd.body)
//	}
//
//	return buf.Bytes()
//}
//
//func decodeRemoteCommand(header, body []byte) *RemotingCommand {
//	decodeLock.Lock()
//	defer decodeLock.Unlock()
//
//	cmd := &RemotingCommand{}
//	cmd.extFields = make(map[string]string)
//	err := json.Unmarshal(header, cmd)
//	if err != nil {
//		log.Print(err)
//		return nil
//	}
//	cmd.body = body
//	return cmd
//}
//
//func CreateRemotingCommand(code int, requestHeader *header.SendMessageRequestHeader) *RemotingCommand {
//	cmd := &RemotingCommand{}
//	cmd.code = code
//	cmd.header = requestHeader
//	cmd.version = 1
//	cmd.opaque = atomic.AddInt32(&requestId, 1) // TODO: safety?
//	return cmd
//}
//
//func (cmd *RemotingCommand) SetBody(body []byte) {
//	cmd.body = body
//}
//
//func (cmd *RemotingCommand) Type() RemotingCommandType {
//	bits := 1 << rpcType
//	if (cmd.flag & bits) == bits {
//		return ResponseCommand
//	}
//	return RqeusetCommand
//}
//
//func (cmd *RemotingCommand) MarkOneWayRpc() {
//	cmd.flag |= (1 << rpcOneWay)
//}
//
//func (cmd *RemotingCommand) String() string {
//	return "" // TODO
//}
