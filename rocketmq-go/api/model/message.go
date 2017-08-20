package rocketmqm

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"

// rocketmq message
// topic: the topic of this message
// tag: the topic of this message, one topic may have no tag or different tag
// key: key makes this message easy to search by console (https://github.com/apache/incubator-rocketmq-externals/rocketmq-console)
// body: the message's user content
// see MessageImpl
type Message interface {
	//
	Topic() (topic string)
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

//create a message instance
func NewMessage() (msg Message) {
	msg = message.NewMessageImpl()
	return
}
