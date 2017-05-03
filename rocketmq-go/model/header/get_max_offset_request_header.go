package header

type GetMaxOffsetRequestHeader struct {
	Topic   string `json:"topic"`
	QueueId int32 `json:"queueId"`
}

func (self *GetMaxOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}