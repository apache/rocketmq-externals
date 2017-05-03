package model

import (
	"math"
	"strconv"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
)

type MessageExt struct {
	*Message
	QueueId                   int32
	StoreSize                 int32
	QueueOffset               int64
	SysFlag                   int32
	BornTimestamp             int64
	BornHost                  string
	StoreTimestamp            int64
	StoreHost                 string
	MsgId                     string
	CommitLogOffset           int64
	BodyCRC                   int32
	ReconsumeTimes            int32
	PreparedTransactionOffset int64

	propertyConsumeStartTimestamp string // race condition
}

func (self *MessageExt) GetOriginMessageId() (string) {
	if self.Properties != nil {
		originMessageId := self.Properties[constant.PROPERTY_ORIGIN_MESSAGE_ID]
		if (len(originMessageId) > 0 ) {
			return originMessageId
		}
	}
	return self.MsgId
}

func (self *MessageExt) GetConsumeStartTime() (int64) {
	if (len(self.propertyConsumeStartTimestamp) > 0 ) {
		return util.StrToInt64WithDefaultValue(self.propertyConsumeStartTimestamp, -1)
	}
	return math.MaxInt64;
}

func (self *MessageExt) SetConsumeStartTime() () {
	if (self.Properties == nil) {
		self.Properties = make(map[string]string)
	}
	nowTime := strconv.FormatInt(time.Now().UnixNano() / 1000000, 10)
	self.Properties[constant.PROPERTY_KEYS] = nowTime
	self.propertyConsumeStartTimestamp = nowTime
	return
}