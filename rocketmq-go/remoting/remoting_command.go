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
	"bytes"
	"encoding/binary"
	"encoding/json"
	"log"
	"os"
	"strconv"
	"sync"
	"sync/atomic"
)

func init() {
	// TODO
}

const (
	SerializeTypeProperty = "rocketmq.serialize.type"
	SerializeTypeEnv      = "ROCKETMQ_SERIALIZE_TYPE"
	RemotingVersionKey    = "rocketmq.remoting.version"
	rpcType               = 0 // 0, request command
	rpcOneWay             = 1 // 0, RPC
)

type RemotingCommandType int

const (
	ResponseCommand RemotingCommandType = iota
	RqeusetCommand
)

var configVersion int = -1
var requestId int32
var decodeLock sync.Mutex

type RemotingCommand struct {
	//header
	Code      int               `json:"code"`
	Language  string            `json:"language"`
	Version   int               `json:"version"`
	Opaque    int32             `json:"opaque"`
	Flag      int               `json:"flag"`
	Remark    string            `json:"remark"`
	ExtFields map[string]string `json:"extFields"`
	header    CustomerHeader    // transient
	//body
	Body []byte `json:"body,omitempty"`
}

func NewRemotingCommand(code int, header CustomerHeader) *RemotingCommand {
	cmd := &RemotingCommand{
		Code:   code,
		header: header,
	}
	setCmdVersion(cmd)
	return cmd
}

func setCmdVersion(cmd *RemotingCommand) {
	if configVersion >= 0 {
		cmd.Version = configVersion // safety
	} else if v := os.Getenv(RemotingVersionKey); v != "" {
		value, err := strconv.Atoi(v)
		if err != nil {
			// TODO log
		}
		cmd.Version = value
		configVersion = value
	}
}

func (cmd *RemotingCommand) encodeHeader() []byte {
	length := 4
	headerData := cmd.buildHeader()
	length += len(headerData)

	if cmd.Body != nil {
		length += len(cmd.Body)
	}

	buf := bytes.NewBuffer([]byte{})
	binary.Write(buf, binary.BigEndian, length)
	binary.Write(buf, binary.BigEndian, len(cmd.Body))
	buf.Write(headerData)

	return buf.Bytes()
}

func (cmd *RemotingCommand) buildHeader() []byte {
	buf, err := json.Marshal(cmd)
	if err != nil {
		return nil
	}
	return buf
}

func (cmd *RemotingCommand) encode() []byte {
	length := 4

	headerData := cmd.buildHeader()
	length += len(headerData)

	if cmd.Body != nil {
		length += len(cmd.Body)
	}

	buf := bytes.NewBuffer([]byte{})
	binary.Write(buf, binary.LittleEndian, length)
	binary.Write(buf, binary.LittleEndian, len(cmd.Body))
	buf.Write(headerData)

	if cmd.Body != nil {
		buf.Write(cmd.Body)
	}

	return buf.Bytes()
}

func decodeRemoteCommand(header, body []byte) *RemotingCommand {
	decodeLock.Lock()
	defer decodeLock.Unlock()

	cmd := &RemotingCommand{}
	cmd.ExtFields = make(map[string]string)
	err := json.Unmarshal(header, cmd)
	if err != nil {
		log.Print(err)
		return nil
	}
	cmd.Body = body
	return cmd
}

func CreateRemotingCommand(code int, requestHeader CustomerHeader) *RemotingCommand {
	cmd := &RemotingCommand{}
	cmd.Code = code
	cmd.header = requestHeader
	cmd.Version = 1
	cmd.Opaque = atomic.AddInt32(&requestId, 1) // TODO: safety?
	return cmd
}

func (cmd *RemotingCommand) SetBody(body []byte) {
	cmd.Body = body
}

func (cmd *RemotingCommand) Type() RemotingCommandType {
	bits := 1 << rpcType
	if (cmd.Flag & bits) == bits {
		return ResponseCommand
	}
	return RqeusetCommand
}

func (cmd *RemotingCommand) MarkOneWayRpc() {
	cmd.Flag |= 1 << rpcOneWay
}

func (cmd *RemotingCommand) String() string {
	return nil // TODO
}
