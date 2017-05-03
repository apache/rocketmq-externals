package header

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

type QueryOffsetResponseHeader struct {
	Offset int64 `json:"offset"`
}

func (self *QueryOffsetResponseHeader) FromMap(headerMap map[string]interface{}) {
	self.Offset = util.StrToInt64WithDefaultValue(headerMap["offset"].(string), -1)
	return
}