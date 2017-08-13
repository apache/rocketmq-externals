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
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/golang/glog"
)

type SerializerHandler struct {
	serializeType rocketmq_api_model.SerializeType
	serializer    Serializer //which serializer this client use, depend on  constant.USE_HEADER_SERIALIZE_TYPE
}

type Serializer interface {
	EncodeHeaderData(request *RemotingCommand) []byte
	DecodeRemoteCommand(header, body []byte) *RemotingCommand
}

var JSON_SERIALIZER = &JsonSerializer{}
var ROCKETMQ_SERIALIZER = &RocketMqSerializer{}

func NewSerializerHandler(serializeType rocketmq_api_model.SerializeType) SerializerHandler {
	serializerHandler := SerializerHandler{serializeType: serializeType}
	switch serializeType {
	case rocketmq_api_model.JSON_SERIALIZE:
		serializerHandler.serializer = JSON_SERIALIZER
		break

	case rocketmq_api_model.ROCKETMQ_SERIALIZE:
		serializerHandler.serializer = ROCKETMQ_SERIALIZER
		break
	default:
		panic("illeage serializer type")
	}
	return serializerHandler
}
func (s *SerializerHandler) EncodeHeader(request *RemotingCommand) []byte {
	length := 4
	headerData := s.serializer.EncodeHeaderData(request)
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

func (s *SerializerHandler) DecodeRemoteCommand(headerSerializableType byte, header, body []byte) *RemotingCommand {
	var serializer Serializer
	switch rocketmq_api_model.SerializeType(headerSerializableType) {
	case rocketmq_api_model.JSON_SERIALIZE:
		serializer = JSON_SERIALIZER
		break
	case rocketmq_api_model.ROCKETMQ_SERIALIZE:
		serializer = ROCKETMQ_SERIALIZER
		break
	default:
		glog.Error("Unknow headerSerializableType", headerSerializableType)
	}
	return serializer.DecodeRemoteCommand(header, body)
}
