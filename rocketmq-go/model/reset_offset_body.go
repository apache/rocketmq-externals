package model

import (
	"encoding/json"
	"github.com/golang/glog"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
)

type ResetOffsetBody struct {
	OffsetTable map[MessageQueue]int64        `json:"offsetTable"`
}

func (self *ResetOffsetBody) Decode(data []byte) (err error) {
	self.OffsetTable = map[MessageQueue]int64{}
	var kvMap map[string]string
	kvMap, err = util.GetKvStringMap(string(data))
	if err != nil {
		return
	}
	glog.Info(kvMap)
	kvMap, err = util.GetKvStringMap(kvMap["\"offsetTable\""])
	if err != nil {
		return
	}
	for k, v := range kvMap {
		messageQueue := &MessageQueue{}
		var offset int64
		err = json.Unmarshal([]byte(k), messageQueue)
		if err != nil {
			return
		}
		offset, err = util.StrToInt64(v)
		if err != nil {
			return
		}
		self.OffsetTable[*messageQueue] = offset
	}
	return
}

