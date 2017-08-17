package rocketmqm
type MessageExt interface {
	//get message topic
	Topic2()(tag string)
	//get message tag
	Tag2() (tag string)

	// get body
	Body2()([]byte)
	MsgId2()(string)
}


