package header

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

type UpdateConsumerOffsetRequestHeader struct {
	ConsumerGroup string `json:"consumerGroup"`
	Topic         string `json:"topic"`
	QueueId       int32  `json:"queueId"`
	CommitOffset  int64  `json:"commitOffset"`
}

func (self *UpdateConsumerOffsetRequestHeader) FromMap(headerMap map[string]interface{}) {
	self.ConsumerGroup = headerMap["consumerGroup"].(string)
	self.QueueId = util.StrToInt32WithDefaultValue(util.ReadString(headerMap["queueId"]), 0)
	self.CommitOffset = util.StrToInt64WithDefaultValue(headerMap["commitOffset"].(string), -1)
	self.Topic = util.ReadString(headerMap["topic"])
	return
}