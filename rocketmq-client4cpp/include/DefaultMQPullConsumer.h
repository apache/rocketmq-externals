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

#ifndef __RMQ_DEFAULTMQPULLCONSUMER_H__
#define __RMQ_DEFAULTMQPULLCONSUMER_H__

#include <list>
#include <string>

#include "RocketMQClient.h"
#include "MQClientException.h"
#include "MessageQueue.h"
#include "MessageExt.h"
#include "ClientConfig.h"
#include "MQPullConsumer.h"

namespace rmq
{
	class OffsetStore;
	class DefaultMQPullConsumerImpl;
	class AllocateMessageQueueStrategy;

	/**
	* Pull Consumer
	*
	*/
	class DefaultMQPullConsumer : public ClientConfig , public MQPullConsumer
	{
	public:
		DefaultMQPullConsumer();
		DefaultMQPullConsumer(const std::string& consumerGroup);
		~DefaultMQPullConsumer();

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
		int getBrokerSuspendMaxTimeMillis() ;
		void setBrokerSuspendMaxTimeMillis(int brokerSuspendMaxTimeMillis);
		std::string getConsumerGroup();
		void setConsumerGroup(const std::string& consumerGroup);
		int getConsumerPullTimeoutMillis();
		void setConsumerPullTimeoutMillis(int consumerPullTimeoutMillis);
		int getConsumerTimeoutMillisWhenSuspend() ;
		void setConsumerTimeoutMillisWhenSuspend(int consumerTimeoutMillisWhenSuspend);
		MessageModel getMessageModel();
		void setMessageModel(MessageModel messageModel);
		MessageQueueListener* getMessageQueueListener();
		void setMessageQueueListener(MessageQueueListener* pMessageQueueListener);
		std::set<std::string> getRegisterTopics();
		void setRegisterTopics( std::set<std::string> registerTopics);

		//MQConsumer
		void sendMessageBack(MessageExt& msg, int delayLevel);
		void sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName);
		std::set<MessageQueue>* fetchSubscribeMessageQueues(const std::string& topic);
		void start();
		void shutdown() ;
		//MQConsumer end

		//MQPullConsumer
		void registerMessageQueueListener(const std::string& topic, MessageQueueListener* pListener);
		PullResult* pull(MessageQueue& mq, const std::string& subExpression, long long offset,int maxNums);
		void pull(MessageQueue& mq,
			const std::string& subExpression,
			long long offset,
			int maxNums,
			PullCallback* pPullCallback);

		PullResult* pullBlockIfNotFound(MessageQueue& mq,
			const std::string& subExpression,
			long long offset,
			int maxNums);

		void pullBlockIfNotFound(MessageQueue& mq,
								 const std::string& subExpression,
								 long long offset,
								 int maxNums,
								 PullCallback* pPullCallback);

		void updateConsumeOffset(MessageQueue& mq, long long offset);

		long long fetchConsumeOffset(MessageQueue& mq, bool fromStore);

		std::set<MessageQueue>* fetchMessageQueuesInBalance(const std::string& topic);
		//MQPullConsumer end

		OffsetStore* getOffsetStore();
		void setOffsetStore(OffsetStore* offsetStore);

		DefaultMQPullConsumerImpl* getDefaultMQPullConsumerImpl();

		bool isUnitMode();
		void setUnitMode(bool isUnitMode);

		int getMaxReconsumeTimes();
		void setMaxReconsumeTimes(int maxReconsumeTimes);

	protected:
		DefaultMQPullConsumerImpl* m_pDefaultMQPullConsumerImpl;

	private:
		std::string m_consumerGroup;
		int m_brokerSuspendMaxTimeMillis ;

		int m_consumerTimeoutMillisWhenSuspend;
		int m_consumerPullTimeoutMillis;

		MessageModel m_messageModel;
		MessageQueueListener* m_pMessageQueueListener;

		OffsetStore* m_pOffsetStore;

		std::set<std::string> m_registerTopics;
		AllocateMessageQueueStrategy* m_pAllocateMessageQueueStrategy;

		/**
	     * Whether the unit of subscription group
	     */
	    bool m_unitMode;

		/**
		 * max retry times£¬default is 15
		 */
	    int m_maxReconsumeTimes;
	};
}

#endif
