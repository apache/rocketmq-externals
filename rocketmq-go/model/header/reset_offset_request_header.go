package header

import (
	"strconv"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
)

type ResetOffsetRequestHeader struct {
	Topic     string `json:"topic"`
	Group     string `json:"group"`
	Timestamp int64  `json:"timestamp"`
	IsForce   bool  `json:"isForce"`
}

func (self *ResetOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.Group = headerMap["group"].(string)
	self.Topic = headerMap["topic"].(string)
	self.Timestamp = util.StrToInt64WithDefaultValue(headerMap["timestamp"].(string), -1)
	self.IsForce, _ = strconv.ParseBool(headerMap["isForce"].(string))
	return
}
