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

#ifndef __DEFAULTMQPRODUCERIMPL_H__
#define __DEFAULTMQPRODUCERIMPL_H__

#include <list>
#include <vector>
#include "MQProducerInner.h"
#include "QueryResult.h"
#include "ServiceState.h"
#include "CommunicationMode.h"
#include "SendResult.h"
#include "MQClientException.h"
#include "Mutex.h"
#include "ScopedLock.h"


namespace rmq
{
    class DefaultMQProducer;
    class SendMessageHook;
    class SendMessageContext;
    class MessageQueue;
    class MessageExt;
    class SendCallback;
    class MessageQueueSelector;
    class MQClientFactory;
    class MQClientException;
    class RemotingException;
    class MQBrokerException;
    class InterruptedException;
    class LocalTransactionExecuter;


    class DefaultMQProducerImpl : public MQProducerInner
    {
    public:
        DefaultMQProducerImpl(DefaultMQProducer* pDefaultMQProducer);
		~DefaultMQProducerImpl();
        void initTransactionEnv();
        void destroyTransactionEnv();

        bool hasHook();
        void registerHook(SendMessageHook* pHook);
        void executeHookBefore(const SendMessageContext& context);
        void executeHookAfter(const SendMessageContext& context);

        void start();
        void start(bool startFactory);
        void shutdown();
        void shutdown(bool shutdownFactory);

        std::set<std::string> getPublishTopicList();
        bool isPublishTopicNeedUpdate(const std::string& topic);

        void checkTransactionState(const std::string& addr,
                                   const MessageExt& msg,
                                   const CheckTransactionStateRequestHeader& checkRequestHeader);

        void updateTopicPublishInfo(const std::string& topic, TopicPublishInfo& info);
        virtual TransactionCheckListener* checkListener();

        void createTopic(const std::string& key, const std::string& newTopic, int queueNum);
        std::vector<MessageQueue>* fetchPublishMessageQueues(const std::string& topic);

        long long searchOffset(const MessageQueue& mq, long long timestamp);
        long long maxOffset(const MessageQueue& mq);
        long long minOffset(const MessageQueue& mq);

        long long earliestMsgStoreTime(const MessageQueue& mq);

        MessageExt* viewMessage(const std::string& msgId);
        QueryResult queryMessage(const std::string& topic,
                                 const std::string& key,
                                 int maxNum,
                                 long long begin,
                                 long long end);

        /**
        * DEFAULT ASYNC -------------------------------------------------------
        */
        void send(Message& msg, SendCallback* sendCallback);
		void send(Message& msg, SendCallback* sendCallback, int timeout);

        /**
        * DEFAULT ONEWAY -------------------------------------------------------
        */
        void sendOneway(Message& msg);

        /**
        * KERNEL SYNC -------------------------------------------------------
        */
        SendResult send(Message& msg, MessageQueue& mq);
		SendResult send(Message& msg, MessageQueue& mq, int timeout);

        /**
        * KERNEL ASYNC -------------------------------------------------------
        */
        void send(Message& msg, MessageQueue& mq, SendCallback* sendCallback);
		void send(Message& msg, MessageQueue& mq, SendCallback* sendCallback, int timeout);

        /**
        * KERNEL ONEWAY -------------------------------------------------------
        */
        void sendOneway(Message& msg, MessageQueue& mq);

        /**
        * SELECT SYNC -------------------------------------------------------
        */
        SendResult send(Message& msg, MessageQueueSelector* selector, void* arg);
		SendResult send(Message& msg, MessageQueueSelector* selector, void* arg, int timeout);

        /**
        * SELECT ASYNC -------------------------------------------------------
        */
        void send(Message& msg, MessageQueueSelector* selector, void* arg, SendCallback* sendCallback);
		void send(Message& msg, MessageQueueSelector* selector, void* arg, SendCallback* sendCallback, int timeout);

        /**
        * SELECT ONEWAY -------------------------------------------------------
        */
        void sendOneway(Message& msg, MessageQueueSelector* selector, void* arg);

		/**
		* SEND with Transaction
		*/
        TransactionSendResult sendMessageInTransaction(Message& msg, LocalTransactionExecuter* tranExecuter, void* arg);

        /**
        * DEFAULT SYNC -------------------------------------------------------
        */
        SendResult send(Message& msg);
		SendResult send(Message& msg, int timeout);

        std::map<std::string, TopicPublishInfo> getTopicPublishInfoTable();

        MQClientFactory* getMQClientFactory();

        int getZipCompressLevel();
        void setZipCompressLevel(int zipCompressLevel);

		ServiceState getServiceState();
		void setServiceState(ServiceState serviceState);

    private:
        SendResult sendSelectImpl(Message& msg,
                                  MessageQueueSelector* selector,
                                  void* pArg,
                                  CommunicationMode communicationMode,
                                  SendCallback* sendCallback,
                                  int timeout);

        SendResult sendDefaultImpl(Message& msg,
                                   CommunicationMode communicationMode,
                                   SendCallback* pSendCallback,
                                   int timeout);

        SendResult sendKernelImpl(Message& msg,
                                  const MessageQueue& mq,
                                  CommunicationMode communicationMode,
                                  SendCallback* pSendCallback,
                                  int timeout);

        void endTransaction(SendResult sendResult,
                            LocalTransactionState localTransactionState,
                            MQClientException localException);

        void makeSureStateOK();
        void checkConfig();

        TopicPublishInfo& tryToFindTopicPublishInfo(const std::string& topic) ;
        bool tryToCompressMessage(Message& msg);

    protected:
        //TODO transaction imp

    private:
        int m_zipCompressLevel;// message compress level, default is 5

        DefaultMQProducer* m_pDefaultMQProducer;

        std::map<std::string, TopicPublishInfo> m_topicPublishInfoTable;
		kpr::RWMutex m_topicPublishInfoTableLock;

        ServiceState m_serviceState;
        MQClientFactory* m_pMQClientFactory;
        std::list<SendMessageHook*> m_hookList;
    };
}

#endif
