package consumer

import (
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
)

type DefaultMQPullConsumer struct {
	consumerStartTimestamp time.Time
	rpcHook remoting.RPCHook
	//consumeMessageHookList
	//filterMessageHookList
	status ServiceStatus

	clientAPI service.MQClientAPI
	offsetStore service.OffsetStore
	rebalance service.Rebalance
}

func NewDefaultMQPullConsumer(hook remoting.RPCHook) DefaultMQPullConsumer {
	return DefaultMQPullConsumer{rpcHook:hook}
}

func (dpc *DefaultMQPullConsumer) CreateTopic(key, topic string, queueNum, topicSysFlag int) error {
	// TODO
	//return dpc.api.CreateTopic(key, topic, queueNum, topicSysFlag)
	return nil
}

func (dpc *DefaultMQPullConsumer) makeSureStatusOK() error {
	return nil
}

func (dpc *DefaultMQPullConsumer) FetchConsumeOffset() error {
	dpc.makeSureStatusOK()

}