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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/golang/glog"
)

//SerializerHandler rocketmq client SerializerHandler
type SerializerHandler struct {
	serializeType rocketmqm.SerializeType
	serializer    Serializer //which serializer this client use, depend on  constant.USE_HEADER_SERIALIZE_TYPE
}

//Serializer rocketmq Serializer
type Serializer interface {
	encodeHeaderData(request *RemotingCommand) []byte
	decodeRemoteCommand(header, body []byte) *RemotingCommand
}

//JSON_SERIALIZER json serializer
var JSON_SERIALIZER = &JsonSerializer{}

//ROCKETMQ_SERIALIZER rocketmq serializer
var ROCKETMQ_SERIALIZER = &RocketMqSerializer{}

func newSerializerHandler(serializeType rocketmqm.SerializeType) SerializerHandler {
	serializerHandler := SerializerHandler{serializeType: serializeType}
	switch serializeType {
	case rocketmqm.JSON_SERIALIZE:
		serializerHandler.serializer = JSON_SERIALIZER
		break

	case rocketmqm.ROCKETMQ_SERIALIZE:
		serializerHandler.serializer = ROCKETMQ_SERIALIZER
		break
	default:
		panic("illeage serializer type")
	}
	return serializerHandler
}
func (s *SerializerHandler) encodeHeader(request *RemotingCommand) []byte {
	length := 4
	headerData := s.serializer.encodeHeaderData(request)
	length += len(headerData)
	if request.Body != nil {
		length += len(request.Body)
	}
	buf := bytes.NewBuffer([]byte{})
	binary.Write(buf, binary.BigEndian, int32(length))                                     // len
	binary.Write(buf, binary.BigEndian, int32(len(headerData)|(int(s.serializeType)<<24))) // header len
	buf.Write(headerData)
	return buf.Bytes()
}

func (s *SerializerHandler) decodeRemoteCommand(headerSerializableType byte, header, body []byte) *RemotingCommand {
	var serializer Serializer
	switch rocketmqm.SerializeType(headerSerializableType) {
	case rocketmqm.JSON_SERIALIZE:
		serializer = JSON_SERIALIZER
		break
	case rocketmqm.ROCKETMQ_SERIALIZE:
		serializer = ROCKETMQ_SERIALIZER
		break
	default:
		glog.Error("Unknow headerSerializableType", headerSerializableType)
	}
	return serializer.decodeRemoteCommand(header, body)
}
