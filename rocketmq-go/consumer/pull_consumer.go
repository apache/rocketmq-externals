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
	brokerName, consumerGroup string) error {
	// TODO

	return nil
}

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
	if _, found := dpc.rebalance.SubscriptionInner()[topic]; !found {
		subscriptionData, err := model.BuildSubscriptionData(topic, model.SubscribeAll)
		if err != nil {
			// TODO log, but can be ignored
		}
		dpc.rebalance.SubscriptionInner()[topic] = subscriptionData // TODO optimize
	}
}

func (dpc *DefaultMQPullConsumer) UnSubscribe(topic string) {
	delete(dpc.rebalance.SubscriptionInner(), topic) // TODO optimize
}

func (dpc *DefaultMQPullConsumer) GroupName() string

func (dpc *DefaultMQPullConsumer) executeHookBefore(ctx model.ConsumeMessageContext) {
	for _, hook := range dpc.consumeMessageHookList {
		hook.DoBefore()
	}
}

func (dpc *DefaultMQPullConsumer) executeHookAfter(ctx model.ConsumeMessageContext) {
	for _, hook := range dpc.consumeMessageHookList {
		hook.DoAfter()
	}
}

func (dpc *DefaultMQPullConsumer) MessageMode() MessageModel

func (dpc *DefaultMQPullConsumer) ConsumeType() ConsumeType {
	return ConsumeActively
}

func (dpc *DefaultMQPullConsumer) ConsumeFromWhere() ConsumeFromWhere {
	return ConsumeFromLastOffset
}

func (dpc *DefaultMQPullConsumer) Subscriptions() []*model.SubscriptionData {
	var result []*model.SubscriptionData
	// TODO
	return result
}

func (dpc *DefaultMQPullConsumer) DoRebalance() {
	if dpc.rebalance != nil {
		dpc.rebalance.DoRebalance(false)
	} else {
		glog.Fatalf("Rebalance Service is nil!")
	}
}

func (dpc *DefaultMQPullConsumer) PersistConsumerOffset() {
	dpc.makeSureStatusOK()

	allocateMQ := dpc.rebalance.ProcessQueueTable()

	var mqs []*message.MessageQueue

	var index = 0 // TODO optimize
	for k := range allocateMQ {
		mqs[index] = &k
		index++
	}
	dpc.offsetStore.PersistAll(mqs)
}

func (dpc *DefaultMQPullConsumer) UpdateTopicSubscribeInfo(topic string, info []*message.MessageQueue) {
	subTable := dpc.rebalance.SubscriptionInner() // TODO optimize
	if subTable != nil {
		dpc.rebalance.TopicSubscribeInfoTable()[topic] = info
	}
}

func (dpc *DefaultMQPullConsumer)  IsSubscribeTopicNeedUpdate(topic string) bool {
	subTable := dpc.rebalance.SubscriptionInner()
	if subTable != nil {
		_, found := subTable[topic]
		if found {
			_, found = dpc.rebalance.TopicSubscribeInfoTable()[topic]
		}
		return found
	}
	return false
}

func (dpc *DefaultMQPullConsumer) UnitMode() bool {
	// TODO
	return false
}

// TODO consider the method position
func (dpc *DefaultMQPullConsumer) ConsumerRunningInfo() runningInfo


func (dpc *DefaultMQPullConsumer) PullAsync(mq *message.MessageQueue, subExp string, offset int64,
	maxNum int, timeout time.Duration, callback model.PullCallback) {
	dpc.pullAsync(mq, subExp, offset, maxNum, false, timeout, callback)
}

func (dpc *DefaultMQPullConsumer) pullAsync(mq *message.MessageQueue, subExp string, offset int64,
	maxNum int, block bool, timeout time.Duration, callback model.PullCallback) {
	dpc.makeSureStatusOK()

	if mq == nil {
		glog.Fatal("MessageQueue is nil!")
	}

	if offset < 0 {
		glog.Fatal("offset < 0!")
	}

	if maxNum <= 0 {
		glog.Fatal("maxNum <= 0")
	}

	if callback == nil {
		glog.Fatal("PullCallback is nil!")
	}

	dpc.subscriptionAutomatically(mq.Topic())

	sysFlag := BuildSysFlag(false, block, true, false)

	subscriptionData, err := model.BuildSubscriptionData(mq.Topic(), subExp)
	// TODO refactor api
	if err != nil {
		glog.Fatal("parse subscription error!")
	}

	// TODO
	//long timeoutMillis = block ? this.defaultMQPullConsumer.getConsumerTimeoutMillisWhenSuspend() : timeout;
	dpc.wrapper.pullKernelImpl(mq,
		subscriptionData.SubString(),
		int64(0),
		offset,
		0,
		maxNum,
		sysFlag,
		time.Second, // TODO this.defaultMQPullConsumer.getBrokerSuspendMaxTimeMillis(), // 8
		timeout,
		remoting.Async,
		callback) // TODO
}

func (dpc *DefaultMQPullConsumer) PullBlockIfNotFound(mq *message.MessageQueue, subExp string, offset int64, maxNum int) model.PullResult {
	return dpc.pullSync(mq, subExp, offset, maxNum, true, time.Second) // TODO optimize time
}

func (dpc *DefaultMQPullConsumer) PullBlockIfNotFoundAsync(mq *message.MessageQueue, subExp string, offset int64,
	maxNum int, callback model.PullCallback) {
	dpc.pullAsync(mq, subExp, offset, maxNum, true, time.Second, callback) // TODO optimize time
}

func (dpc *DefaultMQPullConsumer) Start(){
	switch dpc.status {
	case CreateJust:
		dpc.status = StartFailed

		dpc.checkConfig()
		dpc.copySubscription()

		// TODO
	default:
		glog.Fatalf("The PullConsumer service status not OK, maybe started once. STATUS: %s", dpc.status)
	}
}

func (dpc *DefaultMQPullConsumer) Shutdown() {
	switch dpc.status {
	case Running:
		dpc.PersistConsumerOffset()
		dpc.mqClient.RemoveConsumer("") // TODO topic name
		dpc.clientAPI.Shutdown()
		glog.Infof("The consumer [%s] shutdown successfully.", "name") // TODO name
		dpc.status = ShutdownAlready
	default:
		glog.Warning("The Consumer no Running!")
		break
	}
}

func (dpc *DefaultMQPullConsumer) checkConfig() {
	// TODO
}

func (dpc *DefaultMQPullConsumer) copySubscription() {
	// TODO
}