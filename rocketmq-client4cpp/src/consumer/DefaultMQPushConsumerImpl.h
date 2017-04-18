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

#ifndef __DEFAULTMQPUSHCONSUMERIMPL_H__
#define __DEFAULTMQPUSHCONSUMERIMPL_H__

#include <string>
#include <set>
#include <map>

#include "MQConsumerInner.h"
#include "MessageExt.h"
#include "QueryResult.h"
#include "ServiceState.h"
#include "PullResult.h"
#include "ConsumeMessageHook.h"
#include "MixAll.h"
#include "PullCallback.h"
#include "TimerThread.h"

namespace rmq
{
    class DefaultMQPushConsumer;
    class ConsumeMessageHook;
    class OffsetStore;
    class RebalanceImpl;
    class ConsumerStatManager;
    class ConsumeMessageService;
    class MessageListener;
    class PullRequest;
    class MQClientFactory;
    class PullAPIWrapper;
    class PullMessageService;
    class DefaultMQPushConsumerImplCallback;
	class MQException;

    /**
    * Push Consumer Impl
    *
    */
    class DefaultMQPushConsumerImpl : public  MQConsumerInner
    {
    public:
        DefaultMQPushConsumerImpl(DefaultMQPushConsumer* pDefaultMQPushConsumer);
		~DefaultMQPushConsumerImpl();

        void start();
        void suspend();
        void resume();
        void shutdown();
        bool isPause();
        void setPause(bool pause);

        bool hasHook();
        void registerHook(ConsumeMessageHook* pHook);
        void executeHookBefore(ConsumeMessageContext& context);
        void executeHookAfter(ConsumeMessageContext& context);

        void createTopic(const std::string& key, const std::string& newTopic, int queueNum);
        std::set<MessageQueue>* fetchSubscribeMessageQueues(const std::string& topic);

        long long earliestMsgStoreTime(const MessageQueue& mq);
        long long maxOffset(const MessageQueue& mq);
        long long minOffset(const MessageQueue& mq);
        OffsetStore* getOffsetStore() ;
        void setOffsetStore(OffsetStore* pOffsetStore);

        //MQConsumerInner
        std::string groupName() ;
        MessageModel messageModel() ;
        ConsumeType consumeType();
        ConsumeFromWhere consumeFromWhere();
        std::set<SubscriptionData> subscriptions();
        void doRebalance() ;
        void persistConsumerOffset() ;
        void updateTopicSubscribeInfo(const std::string& topic, const std::set<MessageQueue>& info);
        std::map<std::string, SubscriptionData>& getSubscriptionInner() ;
        bool isSubscribeTopicNeedUpdate(const std::string& topic);

        MessageExt* viewMessage(const std::string& msgId);
        QueryResult queryMessage(const std::string& topic,
                                 const std::string&  key,
                                 int maxNum,
                                 long long begin,
                                 long long end);

        void registerMessageListener(MessageListener* pMessageListener);
        long long searchOffset(const MessageQueue& mq, long long timestamp);
        void sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName);

        void subscribe(const std::string& topic, const std::string& subExpression);
        void unsubscribe(const std::string& topic);

        void updateConsumeOffset(MessageQueue& mq, long long offset);
        void updateCorePoolSize(int corePoolSize);
        bool isConsumeOrderly();
        void setConsumeOrderly(bool consumeOrderly);

        RebalanceImpl* getRebalanceImpl() ;
        MessageListener* getMessageListenerInner();
        DefaultMQPushConsumer* getDefaultMQPushConsumer() ;
        ConsumerStatManager* getConsumerStatManager();

		MQClientFactory* getmQClientFactory();
		void setmQClientFactory(MQClientFactory* mQClientFactory);

		ServiceState getServiceState();
		void setServiceState(ServiceState serviceState);

    private:
        void correctTagsOffset(PullRequest& pullRequest) ;

        void pullMessage(PullRequest* pPullRequest);


        void executePullRequestImmediately(PullRequest* pullRequest);


        void executePullRequestLater(PullRequest* pullRequest, long timeDelay);
		void executeTaskLater(kpr::TimerHandler* handler, long timeDelay);

        void makeSureStateOK();
        void checkConfig();
        void copySubscription() ;
        void updateTopicSubscribeInfoWhenSubscriptionChanged();

    private:
		static const int s_PullTimeDelayMillsWhenException = 3000;
		static const int s_PullTimeDelayMillsWhenFlowControl = 50;
		static const int s_PullTimeDelayMillsWhenSuspend = 1000;
		static const int s_BrokerSuspendMaxTimeMillis = 15000;
		static const int s_ConsumerTimeoutMillisWhenSuspend = 30000;

        long long flowControlTimes1;
        long long flowControlTimes2;
        ServiceState m_serviceState;
        volatile bool m_pause;
        bool m_consumeOrderly;
        DefaultMQPushConsumer* m_pDefaultMQPushConsumer;
        MQClientFactory* m_pMQClientFactory;
        PullAPIWrapper* m_pPullAPIWrapper;
        MessageListener* m_pMessageListenerInner;
        OffsetStore* m_pOffsetStore;
        RebalanceImpl* m_pRebalanceImpl;
        ConsumerStatManager* m_pConsumerStatManager;
        ConsumeMessageService* m_pConsumeMessageService;

        std::list<ConsumeMessageHook*> m_hookList;
        friend class PullMessageService;
        friend class RebalancePushImpl;
        friend class DefaultMQPushConsumerImplCallback;
    };
}

#endif

