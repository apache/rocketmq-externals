package consumer

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
)

type MQConsumer interface {
	// TODO:
	rocketmq.MQAdmin
}

type MQPullConsumer interface {
	MQConsumer // TODO:
}

type MQPushConsumer interface {
	// TODO:
	MQConsumer
}
