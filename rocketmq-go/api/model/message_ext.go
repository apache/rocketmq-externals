package rocketmqm

// see MessageExtImpl
type MessageExt interface {
	//get message topic
	Topic() (tag string)
	//get message tag
	Tag() (tag string)

	// get body
	Body() []byte
	// get messageId
	MsgId() string
}
