package consumer

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"time"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/remoting"
)

type pullAPIWrapper struct {

}

func (p *pullAPIWrapper) processPullResult(mq *message.MessageQueue, pr model.PullResult, subData model.SubscriptionData) model.PullResult
func (p *pullAPIWrapper) updatePullFromWhichNode(mq *message.MessageQueue, brokerID int64)
func (p *pullAPIWrapper) hasHook() bool
func (p *pullAPIWrapper) executeHook(ctx model.FilterMessageContext)

func (p *pullAPIWrapper) pullKernelImpl(mq *message.MessageQueue, subExp string, subVersion, offset, commitOffset int64,
	maxNum, sysFlag int, brokerSuspendMaxTime, timeout time.Duration, mode remoting.CommunicationMode,
	callback model.PullCallback) (model.PullResult, error)

func (p *pullAPIWrapper) recalculatePullFromWhichNode(mq *message.MessageQueue) int64
func (p *pullAPIWrapper) computPullFromWhichFilterServer(topic, brokerAddress string) (string, error)
func (p *pullAPIWrapper) isConnectBrokerByUser() bool
func (p *pullAPIWrapper) setConnectBrokerByUser(connectBrokerByUser bool)
func (p *pullAPIWrapper) randomNum() int
func (p *pullAPIWrapper) registerFilterMessageHook(hooks []model.FilterMessageHook)
func (p *pullAPIWrapper) setDefaultBrokerId(brokerID int64)

