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
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
)

//RocketMqSerializer RocketMqSerializer
type RocketMqSerializer struct {
}

type itemType int8

const (
	keyItem itemType = iota
	valueItem
)

func (r *RocketMqSerializer) encodeHeaderData(cmd *RemotingCommand) []byte {
	var (
		remarkBytes       []byte
		remarkBytesLen    int
		extFieldsBytes    []byte
		extFieldsBytesLen int
	)
	remarkBytesLen = 0
	if len(cmd.Remark) > 0 {
		remarkBytes = []byte(cmd.Remark)
		remarkBytesLen = len(remarkBytes)
	}
	if cmd.ExtFields != nil {
		extFieldsBytes = rocketMqCustomHeaderSerialize(cmd.ExtFields)
		extFieldsBytesLen = len(extFieldsBytes)
	}
	buf := bytes.NewBuffer([]byte{})
	binary.Write(buf, binary.BigEndian, int16(cmd.Code))       //code(~32767) 2
	binary.Write(buf, binary.BigEndian, int8(0))               //JAVA
	binary.Write(buf, binary.BigEndian, int16(cmd.Version))    //2
	binary.Write(buf, binary.BigEndian, int32(cmd.Opaque))     //opaque 4
	binary.Write(buf, binary.BigEndian, int32(cmd.Flag))       //4
	binary.Write(buf, binary.BigEndian, int32(remarkBytesLen)) //4
	if remarkBytesLen > 0 {
		buf.Write(remarkBytes)
	}
	binary.Write(buf, binary.BigEndian, int32(extFieldsBytesLen)) //4
	if extFieldsBytesLen > 0 {
		buf.Write(extFieldsBytes)
	}
	return buf.Bytes()
}

func (r *RocketMqSerializer) decodeRemoteCommand(headerArray, body []byte) (cmd *RemotingCommand) {
	cmd = &RemotingCommand{}
	buf := bytes.NewBuffer(headerArray)
	// int code(~32767)
	binary.Read(buf, binary.BigEndian, &cmd.Code)
	// LanguageCode language
	var LanguageCodeNope byte
	binary.Read(buf, binary.BigEndian, &LanguageCodeNope)
	cmd.Language = constant.REMOTING_COMMAND_LANGUAGE
	// int version(~32767)
	binary.Read(buf, binary.BigEndian, &cmd.Version)
	// int opaque
	binary.Read(buf, binary.BigEndian, &cmd.Opaque)
	// int flag
	binary.Read(buf, binary.BigEndian, &cmd.Flag)
	// String remark
	var remarkLen, extFieldsLen int32
	binary.Read(buf, binary.BigEndian, &remarkLen)
	if remarkLen > 0 {
		var remarkData = make([]byte, remarkLen)
		binary.Read(buf, binary.BigEndian, &remarkData)
		cmd.Remark = string(remarkData)
	}
	binary.Read(buf, binary.BigEndian, &extFieldsLen)
	if extFieldsLen > 0 {
		var extFieldsData = make([]byte, extFieldsLen)
		binary.Read(buf, binary.BigEndian, &extFieldsData)
		extFiledMap := customHeaderDeserialize(extFieldsData)
		cmd.ExtFields = extFiledMap
	}
	cmd.Body = body
	return
}

func rocketMqCustomHeaderSerialize(extFiled map[string]interface{}) (byteData []byte) {
	buf := bytes.NewBuffer([]byte{})
	for key, value := range extFiled {
		keyBytes := []byte(fmt.Sprintf("%v", key))
		valueBytes := []byte(fmt.Sprintf("%v", value))
		binary.Write(buf, binary.BigEndian, int16(len(keyBytes)))
		buf.Write(keyBytes)
		binary.Write(buf, binary.BigEndian, int32(len(valueBytes)))
		buf.Write(valueBytes)
	}
	byteData = buf.Bytes()
	return
}

func customHeaderDeserialize(extFiledDataBytes []byte) (extFiledMap map[string]interface{}) {
	extFiledMap = make(map[string]interface{})
	buf := bytes.NewBuffer(extFiledDataBytes)
	for buf.Len() > 0 {
		var key = getItemFormExtFiledDataBytes(buf, keyItem)
		var value = getItemFormExtFiledDataBytes(buf, valueItem)
		extFiledMap[key] = value
	}
	return
}
func getItemFormExtFiledDataBytes(buff *bytes.Buffer, iType itemType) (item string) {
	if iType == keyItem {
		var length int16
		binary.Read(buff, binary.BigEndian, &length)
		var data = make([]byte, length)
		binary.Read(buff, binary.BigEndian, &data)
		item = string(data)
	}
	if iType == valueItem {
		var length int32
		binary.Read(buff, binary.BigEndian, &length)
		var data = make([]byte, length)
		binary.Read(buff, binary.BigEndian, &data)
		item = string(data)
	}
	return
}
