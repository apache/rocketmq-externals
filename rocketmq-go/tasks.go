package rocketmq

import (
	"math/rand"
	"time"
)

type TaskManager struct {
}

func (self MqClientManager) StartAllScheduledTask() {
	rand.Seed(time.Now().UnixNano())
	go func() {
		updateTopicRouteTimer := time.NewTimer(5 * time.Second)
		for {
			<-updateTopicRouteTimer.C
			self.UpdateTopicRouteInfoFromNameServer()
			updateTopicRouteTimer.Reset(5 * time.Second)
		}
	}()

	go func() {
		heartbeatTimer := time.NewTimer(10 * time.Second)
		for {
			<-heartbeatTimer.C
			self.SendHeartbeatToAllBrokerWithLock()
			heartbeatTimer.Reset(5 * time.Second)
		}
	}()

	go func() {
		rebalanceTimer := time.NewTimer(15 * time.Second)
		for {
			<-rebalanceTimer.C
			self.rebalanceControllr.doRebalance()
			rebalanceTimer.Reset(30 * time.Second)
		}
	}()

	go func() {
		timeoutTimer := time.NewTimer(3 * time.Second)
		for {
			<-timeoutTimer.C
			self.mqClient.ClearExpireResponse()
			timeoutTimer.Reset(time.Second)
		}
	}()
	self.pullMessageController.Start()

	//cleanExpireMsg
	self.cleanExpireMsgController.Start()
}

//
//
//func  updateTopicRouteInfoFromNameServer() {
//	for _, consumer := range self.consumerTable {
//		subscriptions := consumer.subscriptions()
//		for _, subData := range subscriptions {
//			self.updateTopicRouteInfoFromNameServerByTopic(subData.Topic)
//		}
//	}
//}
//func  sendHeartbeatToAllBrokerWithLock() error {
//	heartbeatData := prepareHeartbeatData()
//	if len(heartbeatData.ConsumerDataSet) == 0 {
//		return errors.New("send heartbeat error")
//	}
//
//	self.brokerAddrTableLock.RLock()
//	for _, brokerTable := range self.brokerAddrTable {
//		for brokerId, addr := range brokerTable {
//			if addr == "" || brokerId != "0" {
//				continue
//			}
//			currOpaque := atomic.AddInt32(&opaque, 1)
//			remotingCommand := &RemotingCommand{
//				Code:     HEART_BEAT,
//				Language: "JAVA",
//				Version:  79,
//				Opaque:   currOpaque,
//				Flag:     0,
//			}
//
//			data, err := json.Marshal(*heartbeatData)
//			if err != nil {
//				glog.Error(err)
//				return err
//			}
//			remotingCommand.Body = data
//			glog.V(1).Info("send heartbeat to broker[", addr+"]")
//			response, err := self.remotingClient.invokeSync(addr, remotingCommand, 3000)
//			if err != nil {
//				glog.Error(err)
//			} else {
//				if response == nil || response.Code != SUCCESS {
//					glog.Error("send heartbeat response  error")
//				}
//			}
//		}
//	}
//	self.brokerAddrTableLock.RUnlock()
//	return nil
//}
//
//func  doRebalance() {
//	for _, consumer := range self.consumerTable {
//		consumer.doRebalance()
//	}
//}
//
//func  scanResponseTable() {
//	self.responseTableLock.Lock()
//	for seq, response := range self.responseTable {
//		if  (response.beginTimestamp + 30) <= time.Now().Unix() {
//
//			delete(self.responseTable, seq)
//
//			if response.invokeCallback != nil {
//				response.invokeCallback(nil)
//				glog.Warningf("remove time out request %v", response)
//			}
//		}
//	}
//	self.responseTableLock.Unlock()
//
//}
//
//func  prepareHeartbeatData() *HeartbeatData {
//	heartbeatData := new(HeartbeatData)
//	heartbeatData.ClientId = self.clientId
//	heartbeatData.ConsumerDataSet = make([]*ConsumerData, 0)
//	for group, consumer := range self.consumerTable {
//		consumerData := new(ConsumerData)
//		consumerData.GroupName = group
//		consumerData.ConsumerType = consumer.consumerType
//		consumerData.ConsumeFromWhere = consumer.consumeFromWhere
//		consumerData.MessageModel = consumer.messageModel
//		consumerData.SubscriptionDataSet = consumer.subscriptions()
//		consumerData.UnitMode = consumer.unitMode
//
//		heartbeatData.ConsumerDataSet = append(heartbeatData.ConsumerDataSet, consumerData)
//	}
//	return heartbeatData
//}
