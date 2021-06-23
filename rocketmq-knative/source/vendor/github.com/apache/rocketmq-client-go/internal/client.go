/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package internal

import (
	"context"
	"errors"
	"fmt"
	"net"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/internal/utils"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
)

const (
	clientVersion        = "v2.0.0-alpha3"
	defaultTraceRegionID = "DefaultRegion"

	// tracing message switch
	_TranceOff = "false"

	// Pulling topic information interval from the named server
	_PullNameServerInterval = 30 * time.Second

	// Sending heart beat interval to all broker
	_HeartbeatBrokerInterval = 30 * time.Second

	// Offset persistent interval for consumer
	_PersistOffsetInterval = 5 * time.Second

	// Rebalance interval
	_RebalanceInterval = 20 * time.Second
)

var (
	ErrServiceState = errors.New("service close is not running, please check")

	_VIPChannelEnable = false
)

func init() {
	if os.Getenv("com.rocketmq.sendMessageWithVIPChannel") != "" {
		value, err := strconv.ParseBool(os.Getenv("com.rocketmq.sendMessageWithVIPChannel"))
		if err == nil {
			_VIPChannelEnable = value
		}
	}
}

type InnerProducer interface {
	PublishTopicList() []string
	UpdateTopicPublishInfo(topic string, info *TopicPublishInfo)
	IsPublishTopicNeedUpdate(topic string) bool
	IsUnitMode() bool
}

type InnerConsumer interface {
	PersistConsumerOffset() error
	UpdateTopicSubscribeInfo(topic string, mqs []*primitive.MessageQueue)
	IsSubscribeTopicNeedUpdate(topic string) bool
	SubscriptionDataList() []*SubscriptionData
	Rebalance()
	IsUnitMode() bool
	GetConsumerRunningInfo() *ConsumerRunningInfo
}

func DefaultClientOptions() ClientOptions {
	opts := ClientOptions{
		InstanceName: "DEFAULT",
		RetryTimes:   3,
		ClientIP:     utils.LocalIP,
	}
	return opts
}

type ClientOptions struct {
	GroupName         string
	NameServerAddrs   primitive.NamesrvAddr
	NameServerDomain  string
	Namesrv           *namesrvs
	ClientIP          string
	InstanceName      string
	UnitMode          bool
	UnitName          string
	VIPChannelEnabled bool
	RetryTimes        int
	Interceptors      []primitive.Interceptor
	Credentials       primitive.Credentials
	Namespace         string
}

func (opt *ClientOptions) ChangeInstanceNameToPID() {
	if opt.InstanceName == "DEFAULT" {
		opt.InstanceName = strconv.Itoa(os.Getpid())
	}
}

func (opt *ClientOptions) String() string {
	return fmt.Sprintf("ClientOption [ClientIP=%s, InstanceName=%s, "+
		"UnitMode=%v, UnitName=%s, VIPChannelEnabled=%v]", opt.ClientIP,
		opt.InstanceName, opt.UnitMode, opt.UnitName, opt.VIPChannelEnabled)
}

//go:generate mockgen -source client.go -destination mock_client.go -self_package github.com/apache/rocketmq-client-go/internal  --package internal RMQClient
type RMQClient interface {
	Start()
	Shutdown()

	ClientID() string

	RegisterProducer(group string, producer InnerProducer)
	UnregisterProducer(group string)
	InvokeSync(ctx context.Context, addr string, request *remote.RemotingCommand,
		timeoutMillis time.Duration) (*remote.RemotingCommand, error)
	InvokeAsync(ctx context.Context, addr string, request *remote.RemotingCommand,
		f func(*remote.RemotingCommand, error)) error
	InvokeOneWay(ctx context.Context, addr string, request *remote.RemotingCommand,
		timeoutMillis time.Duration) error
	CheckClientInBroker()
	SendHeartbeatToAllBrokerWithLock()
	UpdateTopicRouteInfo()

	ProcessSendResponse(brokerName string, cmd *remote.RemotingCommand, resp *primitive.SendResult, msgs ...*primitive.Message) error

	RegisterConsumer(group string, consumer InnerConsumer) error
	UnregisterConsumer(group string)
	PullMessage(ctx context.Context, brokerAddrs string, request *PullMessageRequestHeader) (*primitive.PullResult, error)
	RebalanceImmediately()
	UpdatePublishInfo(topic string, data *TopicRouteData, changed bool)
}

var _ RMQClient = new(rmqClient)

type rmqClient struct {
	option ClientOptions
	// group -> InnerProducer
	producerMap sync.Map

	// group -> InnerConsumer
	consumerMap sync.Map
	once        sync.Once

	remoteClient remote.RemotingClient
	hbMutex      sync.Mutex
	close        bool
	rbMutex      sync.Mutex
	namesrvs     *namesrvs
	done         chan struct{}
	shutdownOnce sync.Once
}

var clientMap sync.Map

func GetOrNewRocketMQClient(option ClientOptions, callbackCh chan interface{}) RMQClient {
	client := &rmqClient{
		option:       option,
		remoteClient: remote.NewRemotingClient(),
		namesrvs:     option.Namesrv,
		done:         make(chan struct{}),
	}
	actual, loaded := clientMap.LoadOrStore(client.ClientID(), client)
	if !loaded {
		client.remoteClient.RegisterRequestFunc(ReqNotifyConsumerIdsChanged, func(req *remote.RemotingCommand, addr net.Addr) *remote.RemotingCommand {
			rlog.Info("receive broker's notification to consumer group", map[string]interface{}{
				rlog.LogKeyConsumerGroup: req.ExtFields["consumerGroup"],
			})
			client.RebalanceImmediately()
			return nil
		})
		client.remoteClient.RegisterRequestFunc(ReqCheckTransactionState, func(req *remote.RemotingCommand, addr net.Addr) *remote.RemotingCommand {
			header := new(CheckTransactionStateRequestHeader)
			header.Decode(req.ExtFields)
			msgExts := primitive.DecodeMessage(req.Body)
			if len(msgExts) == 0 {
				rlog.Warning("checkTransactionState, decode message failed", nil)
				return nil
			}
			msgExt := msgExts[0]
			// TODO: add namespace support
			transactionID := msgExt.GetProperty(primitive.PropertyUniqueClientMessageIdKeyIndex)
			if len(transactionID) > 0 {
				msgExt.TransactionId = transactionID
			}
			group := msgExt.GetProperty(primitive.PropertyProducerGroup)
			if group == "" {
				rlog.Warning("checkTransactionState, pick producer group failed", nil)
				return nil
			}
			if option.GroupName != group {
				rlog.Warning("producer group is not equal", nil)
				return nil
			}
			callback := &CheckTransactionStateCallback{
				Addr:   addr,
				Msg:    msgExt,
				Header: *header,
			}
			callbackCh <- callback
			return nil
		})

		client.remoteClient.RegisterRequestFunc(ReqGetConsumerRunningInfo, func(req *remote.RemotingCommand, addr net.Addr) *remote.RemotingCommand {
			rlog.Info("receive get consumer running info request...", nil)
			header := new(GetConsumerRunningInfoHeader)
			header.Decode(req.ExtFields)
			val, exist := clientMap.Load(header.clientID)
			res := remote.NewRemotingCommand(ResError, nil, nil)
			if !exist {
				res.Remark = fmt.Sprintf("Can't find specified client instance of: %s", header.clientID)
			} else {
				cli, ok := val.(*rmqClient)
				var runningInfo *ConsumerRunningInfo
				if ok {
					runningInfo = cli.getConsumerRunningInfo(header.consumerGroup)
				}
				if runningInfo != nil {
					res.Code = ResSuccess
					data, err := runningInfo.Encode()
					if err != nil {
						res.Remark = fmt.Sprintf("json marshal error: %s", err.Error())
					} else {
						res.Body = data
					}
				} else {
					res.Remark = "there is unexpected error when get running info, please check log"
				}
			}
			return res
		})
	}
	return actual.(*rmqClient)
}

func (c *rmqClient) Start() {
	//ctx, cancel := context.WithCancel(context.Background())
	//c.cancel = cancel
	c.once.Do(func() {
		if !c.option.Credentials.IsEmpty() {
			c.remoteClient.RegisterInterceptor(remote.ACLInterceptor(c.option.Credentials))
		}
		// fetchNameServerAddr
		if len(c.option.NameServerAddrs) == 0 {
			go func() {
				// delay
				ticker := time.NewTicker(60 * 2 * time.Second)
				defer ticker.Stop()
				time.Sleep(50 * time.Millisecond)
				for {
					select {
					case <-ticker.C:
						c.namesrvs.UpdateNameServerAddress(c.option.NameServerDomain, c.option.InstanceName)
					case <-c.done:
						rlog.Info("The RMQClient stopping update name server domain info.", map[string]interface{}{
							"clientID": c.ClientID(),
						})
						return
					}
				}
			}()
		}

		// schedule update route info
		go primitive.WithRecover(func() {
			// delay
			ticker := time.NewTicker(_PullNameServerInterval)
			defer ticker.Stop()
			time.Sleep(50 * time.Millisecond)
			for {
				select {
				case <-ticker.C:
					c.UpdateTopicRouteInfo()
				case <-c.done:
					rlog.Info("The RMQClient stopping update topic route info.", map[string]interface{}{
						"clientID": c.ClientID(),
					})
					return
				}
			}
		})

		go primitive.WithRecover(func() {
			ticker := time.NewTicker(_HeartbeatBrokerInterval)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					c.namesrvs.cleanOfflineBroker()
					c.SendHeartbeatToAllBrokerWithLock()
				case <-c.done:
					rlog.Info("The RMQClient stopping clean off line broker and heart beat", map[string]interface{}{
						"clientID": c.ClientID(),
					})
					return
				}
			}
		})

		// schedule persist offset
		go primitive.WithRecover(func() {
			ticker := time.NewTicker(_PersistOffsetInterval)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					c.consumerMap.Range(func(key, value interface{}) bool {
						consumer := value.(InnerConsumer)
						err := consumer.PersistConsumerOffset()
						if err != nil {
							rlog.Error("persist offset failed", map[string]interface{}{
								rlog.LogKeyUnderlayError: err,
							})
						}
						return true
					})
				case <-c.done:
					rlog.Info("The RMQClient stopping persist offset", map[string]interface{}{
						"clientID": c.ClientID(),
					})
					return
				}
			}
		})

		go primitive.WithRecover(func() {
			ticker := time.NewTicker(_RebalanceInterval)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					c.RebalanceImmediately()
				case <-c.done:
					rlog.Info("The RMQClient stopping do rebalance", map[string]interface{}{
						"clientID": c.ClientID(),
					})
					return
				}
			}
		})
	})
}

func (c *rmqClient) Shutdown() {
	c.shutdownOnce.Do(func() {
		close(c.done)
		c.close = true
		c.remoteClient.ShutDown()
	})
}

func (c *rmqClient) ClientID() string {
	id := c.option.ClientIP + "@" + c.option.InstanceName
	if c.option.UnitName != "" {
		id += "@" + c.option.UnitName
	}
	return id
}

func (c *rmqClient) InvokeSync(ctx context.Context, addr string, request *remote.RemotingCommand,
	timeoutMillis time.Duration) (*remote.RemotingCommand, error) {
	if c.close {
		return nil, ErrServiceState
	}
	ctx, _ = context.WithTimeout(ctx, timeoutMillis)
	return c.remoteClient.InvokeSync(ctx, addr, request)
}

func (c *rmqClient) InvokeAsync(ctx context.Context, addr string, request *remote.RemotingCommand,
	f func(*remote.RemotingCommand, error)) error {
	if c.close {
		return ErrServiceState
	}
	return c.remoteClient.InvokeAsync(ctx, addr, request, func(future *remote.ResponseFuture) {
		f(future.ResponseCommand, future.Err)
	})

}

func (c *rmqClient) InvokeOneWay(ctx context.Context, addr string, request *remote.RemotingCommand,
	timeoutMillis time.Duration) error {
	if c.close {
		return ErrServiceState
	}
	return c.remoteClient.InvokeOneWay(ctx, addr, request)
}

func (c *rmqClient) CheckClientInBroker() {
}

// TODO
func (c *rmqClient) SendHeartbeatToAllBrokerWithLock() {
	c.hbMutex.Lock()
	defer c.hbMutex.Unlock()
	hbData := NewHeartbeatData(c.ClientID())

	c.producerMap.Range(func(key, value interface{}) bool {
		pData := producerData{
			GroupName: key.(string),
		}
		hbData.ProducerDatas.Add(pData)
		return true
	})

	c.consumerMap.Range(func(key, value interface{}) bool {
		consumer := value.(InnerConsumer)
		cData := consumerData{
			GroupName:         key.(string),
			CType:             "PUSH",
			MessageModel:      "CLUSTERING",
			Where:             "CONSUME_FROM_FIRST_OFFSET",
			UnitMode:          consumer.IsUnitMode(),
			SubscriptionDatas: consumer.SubscriptionDataList(),
		}
		hbData.ConsumerDatas.Add(cData)
		return true
	})
	if hbData.ProducerDatas.Len() == 0 && hbData.ConsumerDatas.Len() == 0 {
		rlog.Info("sending heartbeat, but no producer and no consumer", nil)
		return
	}
	c.namesrvs.brokerAddressesMap.Range(func(key, value interface{}) bool {
		brokerName := key.(string)
		data := value.(*BrokerData)
		for id, addr := range data.BrokerAddresses {
			cmd := remote.NewRemotingCommand(ReqHeartBeat, nil, hbData.encode())

			ctx, _ := context.WithTimeout(context.Background(), 3*time.Second)
			response, err := c.remoteClient.InvokeSync(ctx, addr, cmd)
			if err != nil {
				rlog.Warning("send heart beat to broker error", map[string]interface{}{
					rlog.LogKeyUnderlayError: err,
				})
				return true
			}
			if response.Code == ResSuccess {
				c.namesrvs.AddBrokerVersion(brokerName, addr, int32(response.Version))
				rlog.Debug("send heart beat to broker success", map[string]interface{}{
					"brokerName": brokerName,
					"brokerId":   id,
					"brokerAddr": addr,
				})
			}
		}
		return true
	})
}

func (c *rmqClient) UpdateTopicRouteInfo() {
	publishTopicSet := make(map[string]bool, 0)
	c.producerMap.Range(func(key, value interface{}) bool {
		producer := value.(InnerProducer)
		list := producer.PublishTopicList()
		for idx := range list {
			publishTopicSet[list[idx]] = true
		}
		return true
	})
	for topic := range publishTopicSet {
		data, changed := c.namesrvs.UpdateTopicRouteInfo(topic)
		c.UpdatePublishInfo(topic, data, changed)
	}

	subscribedTopicSet := make(map[string]bool, 0)
	c.consumerMap.Range(func(key, value interface{}) bool {
		consumer := value.(InnerConsumer)
		list := consumer.SubscriptionDataList()
		for idx := range list {
			subscribedTopicSet[list[idx].Topic] = true
		}
		return true
	})

	for topic := range subscribedTopicSet {
		data, changed := c.namesrvs.UpdateTopicRouteInfo(topic)
		c.updateSubscribeInfo(topic, data, changed)
	}
}

func (c *rmqClient) ProcessSendResponse(brokerName string, cmd *remote.RemotingCommand, resp *primitive.SendResult, msgs ...*primitive.Message) error {
	var status primitive.SendStatus
	switch cmd.Code {
	case ResFlushDiskTimeout:
		status = primitive.SendFlushDiskTimeout
	case ResFlushSlaveTimeout:
		status = primitive.SendFlushSlaveTimeout
	case ResSlaveNotAvailable:
		status = primitive.SendSlaveNotAvailable
	case ResSuccess:
		status = primitive.SendOK
	default:
		status = primitive.SendUnknownError
		return errors.New(cmd.Remark)
	}

	msgIDs := make([]string, 0)
	for i := 0; i < len(msgs); i++ {
		msgIDs = append(msgIDs, msgs[i].GetProperty(primitive.PropertyUniqueClientMessageIdKeyIndex))
	}

	regionId := cmd.ExtFields[primitive.PropertyMsgRegion]
	trace := cmd.ExtFields[primitive.PropertyTraceSwitch]

	if regionId == "" {
		regionId = defaultTraceRegionID
	}

	qId, _ := strconv.Atoi(cmd.ExtFields["queueId"])
	off, _ := strconv.ParseInt(cmd.ExtFields["queueOffset"], 10, 64)

	resp.Status = status
	resp.MsgID = cmd.ExtFields["msgId"]
	resp.OffsetMsgID = cmd.ExtFields["msgId"]
	resp.MessageQueue = &primitive.MessageQueue{
		Topic:      msgs[0].Topic,
		BrokerName: brokerName,
		QueueId:    qId,
	}
	resp.QueueOffset = off
	//TransactionID: sendResponse.TransactionId,
	resp.RegionID = regionId
	resp.TraceOn = trace != "" && trace != _TranceOff
	return nil
}

// PullMessage with sync
func (c *rmqClient) PullMessage(ctx context.Context, brokerAddrs string, request *PullMessageRequestHeader) (*primitive.PullResult, error) {
	cmd := remote.NewRemotingCommand(ReqPullMessage, request, nil)
	ctx, _ = context.WithTimeout(ctx, 30*time.Second)
	res, err := c.remoteClient.InvokeSync(ctx, brokerAddrs, cmd)
	if err != nil {
		return nil, err
	}

	return c.processPullResponse(res)
}

func (c *rmqClient) processPullResponse(response *remote.RemotingCommand) (*primitive.PullResult, error) {

	pullResult := &primitive.PullResult{}
	switch response.Code {
	case ResSuccess:
		pullResult.Status = primitive.PullFound
	case ResPullNotFound:
		pullResult.Status = primitive.PullNoNewMsg
	case ResPullRetryImmediately:
		pullResult.Status = primitive.PullNoMsgMatched
	case ResPullOffsetMoved:
		pullResult.Status = primitive.PullOffsetIllegal
	default:
		return nil, fmt.Errorf("unknown Response Code: %d, remark: %s", response.Code, response.Remark)
	}

	c.decodeCommandCustomHeader(pullResult, response)
	pullResult.SetBody(response.Body)

	return pullResult, nil
}

func (c *rmqClient) decodeCommandCustomHeader(pr *primitive.PullResult, cmd *remote.RemotingCommand) {
	v, exist := cmd.ExtFields["maxOffset"]
	if exist {
		pr.MaxOffset, _ = strconv.ParseInt(v, 10, 64)
	}

	v, exist = cmd.ExtFields["minOffset"]
	if exist {
		pr.MinOffset, _ = strconv.ParseInt(v, 10, 64)
	}

	v, exist = cmd.ExtFields["nextBeginOffset"]
	if exist {
		pr.NextBeginOffset, _ = strconv.ParseInt(v, 10, 64)
	}

	v, exist = cmd.ExtFields["suggestWhichBrokerId"]
	if exist {
		pr.SuggestWhichBrokerId, _ = strconv.ParseInt(v, 10, 64)
	}
}

func (c *rmqClient) RegisterConsumer(group string, consumer InnerConsumer) error {
	_, exist := c.consumerMap.Load(group)
	if exist {
		rlog.Warning("the consumer group exist already", map[string]interface{}{
			rlog.LogKeyConsumerGroup: group,
		})
		return fmt.Errorf("the consumer group exist already")
	}
	c.consumerMap.Store(group, consumer)
	return nil
}

func (c *rmqClient) UnregisterConsumer(group string) {
	c.consumerMap.Delete(group)
}

func (c *rmqClient) RegisterProducer(group string, producer InnerProducer) {
	c.producerMap.Store(group, producer)
}

func (c *rmqClient) UnregisterProducer(group string) {
	c.producerMap.Delete(group)
}

func (c *rmqClient) RebalanceImmediately() {
	c.rbMutex.Lock()
	defer c.rbMutex.Unlock()
	c.consumerMap.Range(func(key, value interface{}) bool {
		consumer := value.(InnerConsumer)
		consumer.Rebalance()
		return true
	})
}

func (c *rmqClient) UpdatePublishInfo(topic string, data *TopicRouteData, changed bool) {
	if data == nil {
		return
	}

	c.producerMap.Range(func(key, value interface{}) bool {
		p := value.(InnerProducer)
		updated := changed
		if !updated {
			updated = p.IsPublishTopicNeedUpdate(topic)
		}
		if updated {
			publishInfo := c.namesrvs.routeData2PublishInfo(topic, data)
			publishInfo.HaveTopicRouterInfo = true
			p.UpdateTopicPublishInfo(topic, publishInfo)
		}
		return true
	})
}

func (c *rmqClient) updateSubscribeInfo(topic string, data *TopicRouteData, changed bool) {
	if data == nil {
		return
	}
	if !c.isNeedUpdateSubscribeInfo(topic) {
		return
	}
	c.consumerMap.Range(func(key, value interface{}) bool {
		consumer := value.(InnerConsumer)
		updated := changed
		if !updated {
			updated = consumer.IsSubscribeTopicNeedUpdate(topic)
		}
		if updated {
			consumer.UpdateTopicSubscribeInfo(topic, routeData2SubscribeInfo(topic, data))
		}

		return true
	})
}

func (c *rmqClient) isNeedUpdateSubscribeInfo(topic string) bool {
	var result bool
	c.consumerMap.Range(func(key, value interface{}) bool {
		consumer := value.(InnerConsumer)
		if consumer.IsSubscribeTopicNeedUpdate(topic) {
			result = true
			return false
		}
		return true
	})
	return result
}

func (c *rmqClient) getConsumerRunningInfo(group string) *ConsumerRunningInfo {
	consumer, exist := c.consumerMap.Load(group)
	if !exist {
		return nil
	}
	info := consumer.(InnerConsumer).GetConsumerRunningInfo()
	if info != nil {
		info.Properties[PropClientVersion] = clientVersion
	}
	return info
}

func routeData2SubscribeInfo(topic string, data *TopicRouteData) []*primitive.MessageQueue {
	list := make([]*primitive.MessageQueue, 0)
	for idx := range data.QueueDataList {
		qd := data.QueueDataList[idx]
		if queueIsReadable(qd.Perm) {
			for i := 0; i < qd.ReadQueueNums; i++ {
				list = append(list, &primitive.MessageQueue{
					Topic:      topic,
					BrokerName: qd.BrokerName,
					QueueId:    i,
				})
			}
		}
	}
	return list
}

func brokerVIPChannel(brokerAddr string) string {
	if !_VIPChannelEnable {
		return brokerAddr
	}
	var brokerAddrNew strings.Builder
	ipAndPort := strings.Split(brokerAddr, ":")
	port, err := strconv.Atoi(ipAndPort[1])
	if err != nil {
		return ""
	}
	brokerAddrNew.WriteString(ipAndPort[0])
	brokerAddrNew.WriteString(":")
	brokerAddrNew.WriteString(strconv.Itoa(port - 2))
	return brokerAddrNew.String()
}
