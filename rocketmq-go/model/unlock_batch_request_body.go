package model

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"

type LockBatchRequestBody struct {
	ConsumerGroup string
	ClientID string
	MqSet *util.Set
}

func LockBatchRequestBodyDecode(bs []byte) LockBatchRequestBody

type UnlockBatchRequestBody struct {
	ConsumerGroup string
	ClientID string
	MqSet *util.Set
}

func(b *UnlockBatchRequestBody) Encode() []byte