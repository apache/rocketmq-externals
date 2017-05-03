package header

type QueryConsumerOffsetRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
	Topic         string `json:"topic"`
	QueueId       int32  `json:"queueId"`
}

func (self *QueryConsumerOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}