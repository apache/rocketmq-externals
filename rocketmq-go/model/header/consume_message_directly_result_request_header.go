package header

type ConsumeMessageDirectlyResultRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
	ClientId      string `json:"clientId"`
	MsgId         string `json:"msgId"`
	BrokerName    string `json:"brokerName"`
}

func (self *ConsumeMessageDirectlyResultRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.ConsumerGroup = headerMap["consumerGroup"].(string)
	self.ClientId = headerMap["clientId"].(string)
	self.MsgId = headerMap["msgId"].(string)
	self.BrokerName = headerMap["brokerName"].(string)
	return
}