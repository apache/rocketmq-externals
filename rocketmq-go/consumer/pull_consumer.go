package consumer

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"time"
	"github.com/golang/glog"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
)

type DefaultMQPullConsumer struct {
	consumerStartTimestamp time.Time
	rpcHook                remoting.RPCHook
	consumeMessageHookList []model.ConsumerHook // TODO consumeMessageHook
	filterMessageHookList  []model.ConsumerHook // TODO filterMessageHook
	status                 ServiceStatus
	mqClient               *service.MQClient
	clientAPI              *service.MQClientAPI
	offsetStore            service.OffsetStore
	rebalance              service.Rebalance
}

func NewDefaultMQPullConsumer(hook remoting.RPCHook) DefaultMQPullConsumer {
	return DefaultMQPullConsumer{rpcHook: hook, rebalance: service.PullMessageRebalance{}}
}

func (dpc *DefaultMQPullConsumer) RegisterConsumeMessageHook(hook model.ConsumerHook) {
	// TODO optimize
	dpc.consumeMessageHookList[len(dpc.consumeMessageHookList)] = hook
	glog.Infof("register consumeMessageHook Hook, %s", hook.Name())
}

func (dpc *DefaultMQPullConsumer) CreateTopic(key, topic string, queueNum, topicSysFlag int) error {
	if err := dpc.makeSureStatusOK(); err != nil {
		return err
	}
	// TODO
	//return dpc.api.CreateTopic(key, topic, queueNum, topicSysFlag)
	return nil
}

func (dpc *DefaultMQPullConsumer) makeSureStatusOK() error {
	if dpc.status != Running {
		return model.NewMQClientError(-1, fmt.Sprintf("The consumer service state not OK, %s", dpc.status))
	}
	return nil
}

func (dpc *DefaultMQPullConsumer) FetchConsumeOffset(mq *message.MessageQueue, readType service.ReadOffsetType) (int64, error) {
	if err := dpc.makeSureStatusOK(); err != nil {
		return -1, err
	}

	return dpc.offsetStore.ReadOffset(mq, readType)
}
