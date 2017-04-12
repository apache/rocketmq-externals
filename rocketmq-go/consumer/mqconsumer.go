package consumer

import ptc "../protocol"

type MQConsumer interface {
	// TODO:
	ptc.MQAdmin
}

type MQPullConsumer interface {
	MQConsumer // TODO:
}

type MQPushConsumer interface {
	// TODO:
	MQConsumer
}
