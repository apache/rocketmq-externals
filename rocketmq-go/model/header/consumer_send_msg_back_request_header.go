package header

type ConsumerSendMsgBackRequestHeader struct {
	Offset            int64
	Group             string
	DelayLevel        int32
	OriginMsgId       string
	OriginTopic       string
	UnitMode          bool
	MaxReconsumeTimes int32
}

func (self *ConsumerSendMsgBackRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}
