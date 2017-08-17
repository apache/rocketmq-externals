package rocketmqm

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"

type Message interface {
	//set message topic
	SetTopic(tag string)
	//set message tag
	SetTag(tag string)
	//get message tag
	Tag() (tag string)
	//set message key
	SetKeys(keys []string)
	// set delay time level
	SetDelayTimeLevel(delayTimeLevel int)
	// set body
	SetBody([]byte)
}

func NewMessage() (msg Message) {
	msg = message.NewMessageImpl()
	return
}
