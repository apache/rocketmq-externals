package header

type GetConsumerRunningInfoRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
	ClientId      string `json:"clientId"`
	JstackEnable  bool `json:"jstackEnable"`
}

func (self *GetConsumerRunningInfoRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.ConsumerGroup = headerMap["consumerGroup"].(string)
	self.ClientId = headerMap["clientId"].(string)
	return
}