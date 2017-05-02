package consumer

import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
	"github.com/golang/glog"
	"time"
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

	wrapper pullAPIWrapper
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
	dpc.makeSureStatusOK()
	// TODO
	//return dpc.api.CreateTopic(key, topic, queueNum, topicSysFlag)
	return nil
}

func (dpc *DefaultMQPullConsumer) makeSureStatusOK() {
	if dpc.status != Running {
		glog.Fatal(model.NewMQClientError(-1, fmt.Sprintf("The consumer service state not OK, %s", dpc.status)))
	}
}

func (dpc *DefaultMQPullConsumer) FetchConsumeOffset(mq *message.MessageQueue, readType service.ReadOffsetType) (int64, error) {
	dpc.makeSureStatusOK()
	return dpc.offsetStore.ReadOffset(mq, readType)
}

func (dpc *DefaultMQPullConsumer) FetchMessageQueuesInBalance(topic string) ([]*message.MessageQueue, error) {
	dpc.makeSureStatusOK()
	// TODO
	return nil, nil
}

func (dpc *DefaultMQPullConsumer) SendMessageBack(msg message.MessageExt, delayLevel int,
	brokerName, consumerGroup string) error

func (dpc *DefaultMQPullConsumer) Pull(mq *message.MessageQueue, subExp string, offset int64,
	maxNum int, timeout time.Duration) model.PullResult {
	return dpc.pullSync(mq, subExp, offset, maxNum, false, timeout)
}

func (dpc *DefaultMQPullConsumer) pullSync(mq *message.MessageQueue, subExp string, offset int64,
	maxNum int, block bool, timeout time.Duration) model.PullResult {
	dpc.makeSureStatusOK()

	if mq == nil {
		glog.Fatal("MessageQueue is nil!")
	}

	if offset < 0 {
		glog.Fatal("offset < 0!")
	}

	if maxNum <= 0 {
		glog.Fatalf("maxNum <= 0")
	}

	dpc.subscriptionAutomatically(mq.Topic())

	sysFlag := BuildSysFlag(false, block, true, false)
	subscriptionData, err := model.BuildSubscriptionData(mq.Topic(), subExp)

	// TODO refactor api
	if err != nil {
		// TODO log
	}

	// TODO
	//long timeoutMillis = block ? this.defaultMQPullConsumer.getConsumerTimeoutMillisWhenSuspend() : timeout;
	pullResult, err := dpc.wrapper.pullKernelImpl(mq,
		subscriptionData.SubString(),
		int64(0),
		offset,
		0,
		maxNum,
		sysFlag,
		time.Second, // TODO
		timeout,
		remoting.Sync,
		nil)

	if err != nil {
		glog.Errorf("PullMessage Error! Topic %s, BrokerName: %s", mq.Topic(),mq.BrokerName())
	}
	dpc.wrapper.processPullResult(mq, pullResult, subscriptionData)

	if dpc.consumeMessageHookList != nil && len(dpc.consumeMessageHookList) > 0 {
		// TODO
	}
	return pullResult
}

func (dpc *DefaultMQPullConsumer) subscriptionAutomatically(topic string) {
	// TODO
}
