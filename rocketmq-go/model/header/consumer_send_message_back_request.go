package header

type ConsumerSendMsgBackRequestHeader struct {
	Offset int64
	ConsumerGroup string
	DelayLevel int
	OriginMsgID string
	OriginTopic string
	UnitMode bool
	MaxReconsumeTimes int
}

func (header *ConsumerSendMsgBackRequestHeader) FromMap(headerMap map[string]interface{}) {

}
