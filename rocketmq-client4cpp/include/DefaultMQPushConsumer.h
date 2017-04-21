/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef __RMQ_DEFAULTMQPUSHCONSUMER_H__
#define __RMQ_DEFAULTMQPUSHCONSUMER_H__

#include <list>
#include <string>

#include "RocketMQClient.h"
#include "MQClientException.h"
#include "Message.h"
#include "MessageExt.h"
#include "MessageQueue.h"
#include "MessageListener.h"
#include "PullResult.h"
#include "ClientConfig.h"
#include "MQPushConsumer.h"

namespace rmq
{
	class AllocateMessageQueueStrategy;
	class DefaultMQPushConsumerImpl;
	class OffsetStore;

	/**
	* Push Consumer
	*
	*/
	class DefaultMQPushConsumer : public ClientConfig ,public MQPushConsumer
	{
	public:
		DefaultMQPushConsumer();
		DefaultMQPushConsumer(const std::string& consumerGroup);
		~DefaultMQPushConsumer();

		//MQAdmin
		void createTopic(const std::string& key, const std::string& newTopic, int queueNum);
		long long searchOffset(const MessageQueue& mq, long long timestamp);
		long long maxOffset(const MessageQueue& mq);
		long long minOffset(const MessageQueue& mq);
		long long earliestMsgStoreTime(const MessageQueue& mq);
		MessageExt* viewMessage(const std::string& msgId);
		QueryResult queryMessage(const std::string& topic,
								 const std::string&  key,
								 int maxNum,
								 long long begin,
								 long long end);

		// MQadmin end

		AllocateMessageQueueStrategy* getAllocateMessageQueueStrategy();
		void setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy);

		int getConsumeConcurrentlyMaxSpan();
		void setConsumeConcurrentlyMaxSpan(int consumeConcurrentlyMaxSpan);

		ConsumeFromWhere getConsumeFromWhere();
		void setConsumeFromWhere(ConsumeFromWhere consumeFromWhere);

		int getConsumeMessageBatchMaxSize();
		void setConsumeMessageBatchMaxSize(int consumeMessageBatchMaxSize);

		std::string getConsumerGroup();
		void setConsumerGroup(const std::string& consumerGroup) ;

		int getConsumeThreadMax() ;
		void setConsumeThreadMax(int consumeThreadMax);

		int getConsumeThreadMin();
		void setConsumeThreadMin(int consumeThreadMin);

		MessageListener* getMessageListener();
		void setMessageListener(MessageListener* pMessageListener);

		MessageModel getMessageModel();
		void setMessageModel(MessageModel messageModel) ;

		int getPullBatchSize() ;
		void setPullBatchSize(int pullBatchSize);

		long getPullInterval();
		void setPullInterval(long pullInterval);

		int getPullThresholdForQueue();
		void setPullThresholdForQueue(int pullThresholdForQueue);

		std::map<std::string, std::string>& getSubscription();
		void setSubscription(const std::map<std::string, std::string>& subscription);

		//MQConsumer
		void sendMessageBack(MessageExt& msg, int delayLevel);
		void sendMessageBack(MessageExt& msg, int delayLevel, const std::string brokerName);
		std::set<MessageQueue>* fetchSubscribeMessageQueues(const std::string& topic);

		void start();
		void shutdown();
		//MQConsumer end

		//MQPushConsumer
		void registerMessageListener(MessageListener* pMessageListener);

		void subscribe(const std::string& topic, const std::string& subExpression);
		void unsubscribe(const std::string& topic);

		void updateCorePoolSize(int corePoolSize);

		void suspend() ;
		void resume();
		//MQPushConsumer end

		OffsetStore* getOffsetStore();
		void setOffsetStore(OffsetStore* offsetStore);

		std::string getConsumeTimestamp();
	    void setConsumeTimestamp(std::string consumeTimestamp);

		DefaultMQPushConsumerImpl* getDefaultMQPushConsumerImpl();

		bool isPostSubscriptionWhenPull();
		void setPostSubscriptionWhenPull(bool postSubscriptionWhenPull);

		bool isUnitMode();
		void setUnitMode(bool isUnitMode);

		int getMaxReconsumeTimes();
		void setMaxReconsumeTimes(int maxReconsumeTimes);

		int getSuspendCurrentQueueTimeMillis();
		void setSuspendCurrentQueueTimeMillis(int suspendCurrentQueueTimeMillis);

		int getConsumeTimeout();
		void setConsumeTimeout(int consumeTimeout);

	protected:
		DefaultMQPushConsumerImpl* m_pDefaultMQPushConsumerImpl;

	private:
		std::string m_consumerGroup;
		MessageModel m_messageModel;
		ConsumeFromWhere m_consumeFromWhere;
		std::string m_consumeTimestamp;

		AllocateMessageQueueStrategy* m_pAllocateMessageQueueStrategy ;
		std::map<std::string /* topic */, std::string /* sub expression */> m_subscription ;

		MessageListener* m_pMessageListener;
		OffsetStore* m_pOffsetStore;

		int m_consumeThreadMin;
		int m_consumeThreadMax;

		int m_consumeConcurrentlyMaxSpan;
		int m_pullThresholdForQueue;
		long m_pullInterval;

		int m_consumeMessageBatchMaxSize;
		int m_pullBatchSize;

	    bool m_postSubscriptionWhenPull;
	    bool m_unitMode;
	    int m_maxReconsumeTimes;

	    long m_suspendCurrentQueueTimeMillis;
	    long m_consumeTimeout;
	};
}

#endif
