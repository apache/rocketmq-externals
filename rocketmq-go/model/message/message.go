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
package message

import (
	"bytes"
	"compress/zlib"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"github.com/golang/glog"
	"io/ioutil"
)

const (
	CompressedFlag          = 1 << 0
	MultiTagsFlag           = 1 << 1
	TransactionNotType      = 0 << 2
	TransactionPreparedType = 1 << 2
	TransactionCommitType   = 2 << 2
	TransactionRollbackType = 3 << 2
)

const (
	NameValueSeparator = 1 + iota
	PropertySeparator
)

const (
	CharacterMaxLength = 255
)

type Message struct {
	Topic      string
	Flag       int32
	properties map[string]string
	Body       []byte
}

func NewDefultMessage(topic string, body []byte) *Message {
	return NewMessage(topic, "", "", 0, body, true)
}

type MessageExt struct {
	Message
	QueueId       int32
	StoreSize     int32
	QueueOffset   int64
	SysFlag       int32
	BornTimestamp int64
	// bornHost
	StoreTimestamp int64
	// storeHost
	MsgId                     string
	CommitLogOffset           int64
	BodyCRC                   int32
	ReconsumeTimes            int32
	PreparedTransactionOffset int64
}

func (msg *Message) encodeMessage() []byte {
	// TODO
	return nil
}

func decodeMessage(data []byte) []*MessageExt {
	buf := bytes.NewBuffer(data)
	var storeSize, magicCode, bodyCRC, queueId, flag, sysFlag, reconsumeTimes, bodyLength, bornPort, storePort int32
	var queueOffset, physicOffset, preparedTransactionOffset, bornTimeStamp, storeTimestamp int64
	var topicLen byte
	var topic, body, properties, bornHost, storeHost []byte
	var propertiesLength int16

	var propertiesMap map[string]string

	msgs := make([]*MessageExt, 0, 32)
	for buf.Len() > 0 {
		msg := new(MessageExt)
		binary.Read(buf, binary.BigEndian, &storeSize)
		binary.Read(buf, binary.BigEndian, &magicCode)
		binary.Read(buf, binary.BigEndian, &bodyCRC)
		binary.Read(buf, binary.BigEndian, &queueId)
		binary.Read(buf, binary.BigEndian, &flag)
		binary.Read(buf, binary.BigEndian, &queueOffset)
		binary.Read(buf, binary.BigEndian, &physicOffset)
		binary.Read(buf, binary.BigEndian, &sysFlag)
		binary.Read(buf, binary.BigEndian, &bornTimeStamp)
		bornHost = make([]byte, 4)
		binary.Read(buf, binary.BigEndian, &bornHost)
		binary.Read(buf, binary.BigEndian, &bornPort)
		binary.Read(buf, binary.BigEndian, &storeTimestamp)
		storeHost = make([]byte, 4)
		binary.Read(buf, binary.BigEndian, &storeHost)
		binary.Read(buf, binary.BigEndian, &storePort)
		binary.Read(buf, binary.BigEndian, &reconsumeTimes)
		binary.Read(buf, binary.BigEndian, &preparedTransactionOffset)
		binary.Read(buf, binary.BigEndian, &bodyLength)
		if bodyLength > 0 {
			body = make([]byte, bodyLength)
			binary.Read(buf, binary.BigEndian, body)

			if (sysFlag & CompressedFlag) == CompressedFlag {
				b := bytes.NewReader(body)
				z, err := zlib.NewReader(b)
				if err != nil {
					fmt.Println(err)
					return nil
				}

				body, err = ioutil.ReadAll(z)
				if err != nil {
					fmt.Println(err)
					return nil
				}
				z.Close()
			}

		}
		binary.Read(buf, binary.BigEndian, &topicLen)
		topic = make([]byte, 0)
		binary.Read(buf, binary.BigEndian, &topic)
		binary.Read(buf, binary.BigEndian, &propertiesLength)
		if propertiesLength > 0 {
			properties = make([]byte, propertiesLength)
			binary.Read(buf, binary.BigEndian, &properties)
			propertiesMap = make(map[string]string)
			json.Unmarshal(properties, &propertiesMap)
		}

		if magicCode != -626843481 {
			fmt.Printf("magic code is error %d", magicCode)
			return nil
		}

		msg.Topic = string(topic)
		msg.QueueId = queueId
		msg.SysFlag = sysFlag
		msg.QueueOffset = queueOffset
		msg.BodyCRC = bodyCRC
		msg.StoreSize = storeSize
		msg.BornTimestamp = bornTimeStamp
		msg.ReconsumeTimes = reconsumeTimes
		msg.Flag = flag
		//msg.commitLogOffset=physicOffset
		msg.StoreTimestamp = storeTimestamp
		msg.PreparedTransactionOffset = preparedTransactionOffset
		msg.Body = body
		msg.properties = propertiesMap

		msgs = append(msgs, msg)
	}

	return msgs
}

func messageProperties2String(properties map[string]string) string {
	StringBuilder := bytes.NewBuffer([]byte{})
	if properties != nil && len(properties) != 0 {
		for k, v := range properties {
			binary.Write(StringBuilder, binary.BigEndian, k)                  // 4
			binary.Write(StringBuilder, binary.BigEndian, NameValueSeparator) // 4
			binary.Write(StringBuilder, binary.BigEndian, v)                  // 4
			binary.Write(StringBuilder, binary.BigEndian, PropertySeparator)  // 4
		}
	}
	return StringBuilder.String()
}

//func (msg Message) checkMessage(producer *DefaultProducer) (err error) {
//	if err = checkTopic(msg.Topic); err != nil {
//		if len(msg.Body) == 0 {
//			err = errors.New("ResponseCode:" + strconv.Itoa(MsgIllegal) + ", the message body is null")
//		} else if len(msg.Body) > producer.maxMessageSize {
//			err = errors.New("ResponseCode:" + strconv.Itoa(MsgIllegal) + ", the message body size over max value, MAX:" + strconv.Itoa(producer.maxMessageSize))
//		}
//	}
//	return
//}

//func checkTopic(topic string) (err error) {
//	if topic == "" {
//		err = errors.New("the specified topic is blank")
//	}
//	if len(topic) > CharacterMaxLength {
//		err = errors.New("the specified topic is longer than topic max length 255")
//	}
//	if topic == DefaultTopic {
//		err = errors.New("the topic[" + topic + "] is conflict with default topic")
//	}
//	return
//}

func NewMessage(topic, tags, keys string, flag int32, body []byte, waitStoreMsgOK bool) *Message {
	message := &Message{
		Topic: topic,
		Flag:  flag,
		Body:  body,
	}

	if tags != "" {
		message.SetTags(tags)
	}

	if keys != "" {
		message.SetKeys(keys)
	}

	message.SetWaitStoreMsgOK(waitStoreMsgOK)
	return message
}

func (msg *Message) SetTags(t string) {
	msg.putProperty(MessageConst.PropertyTags, t)
}

func (msg *Message) SetKeys(k string) {
	msg.putProperty(MessageConst.PropertyKeys, k)
}

func (msg *Message) SetWaitStoreMsgOK(b bool) {

}

func (msg *Message) Property() map[string]string {
	return msg.properties
}

func (msg *Message) putProperty(k, v string) {
	if msg.properties == nil {
		msg.properties = make(map[string]string)
	}
	if v, found := msg.properties[k]; !found {
		msg.properties[k] = v
	} else {
		glog.Infof("Message put peoperties key: %s existed.", k)
	}
}

func (msg *Message) removeProperty(k, v string) string {
	if v, ok := msg.properties[k]; ok {
		delete(msg.properties, k)
		return v
	}
	return ""
}

func (msg *Message) String() string {
	return fmt.Sprintf("Message [topic=%s, flag=%s, properties=%s, body=%s]",
		msg.Topic, msg.Flag, msg.properties, msg.Body)
}
