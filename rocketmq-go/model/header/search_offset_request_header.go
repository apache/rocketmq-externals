package header

import (
)
import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

type SearchOffsetRequestHeader struct {
	Topic     string       `json:"topic"`
	QueueId   int32        `json:"queueId"`
	Timestamp int64        `json:"timestamp"`
}

func (self *SearchOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.Topic = headerMap["topic"].(string)
	self.Topic = headerMap["queueId"].(string)
	self.Timestamp = util.StrToInt64WithDefaultValue(headerMap["timestamp"].(string), -1)
	return
}