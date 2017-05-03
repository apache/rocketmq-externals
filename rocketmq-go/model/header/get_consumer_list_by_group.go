package header

type GetConsumerListByGroupRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
}

func (self *GetConsumerListByGroupRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}

type GetConsumerListByGroupResponseBody struct {
	ConsumerIdList []string
}

func (self *GetConsumerListByGroupResponseBody) FromMap(headerMap map[string]interface{}) {
	return
}