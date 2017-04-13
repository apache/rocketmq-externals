package rocketmq

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/message"
)

type QueryResult struct {
	indexLastUpdateTimestamp int64
	messageList              []*msg.MessageExt
}

func NewQueryResult(timestamp int64, list []*msg.MessageExt) *QueryResult {
	return &QueryResult{
		indexLastUpdateTimestamp: timestamp,
		messageList:              list,
	}
}

func (qr *QueryResult) IndexLastUpdateTimestamp() int64 {
	return qr.indexLastUpdateTimestamp
}

func (qr *QueryResult) MessageList() []*msg.MessageExt { //TODO: address?
	return qr.messageList
}

func (qr *QueryResult) String() string {
	return fmt.Sprintf("QueryResult [indexLastUpdateTimestamp=%s, messageList=%s]",
		qr.indexLastUpdateTimestamp, qr.messageList)
}
