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
#ifndef __TRANSACTIONMQPRODUCER_H__
#define __TRANSACTIONMQPRODUCER_H__

#include "DefaultMQProducer.h"
#include "DefaultMQProducerImpl.h"
#include "MQClientException.h"

namespace rmq
{
    class TransactionMQProducer : public DefaultMQProducer
    {
    public:
        TransactionMQProducer()
            : m_pTransactionCheckListener(NULL),
              m_checkThreadPoolMinSize(1),
              m_checkThreadPoolMaxSize(1),
              m_checkRequestHoldMax(2000)
        {

        }

        TransactionMQProducer(const std::string& producerGroup)
            : DefaultMQProducer(producerGroup),
              m_pTransactionCheckListener(NULL),
              m_checkThreadPoolMinSize(1),
              m_checkThreadPoolMaxSize(1),
              m_checkRequestHoldMax(2000)
        {

        }

        void start()
        {
            m_pDefaultMQProducerImpl->initTransactionEnv();
            DefaultMQProducer::start();
        }

        void shutdown()
        {
            DefaultMQProducer::shutdown();
            m_pDefaultMQProducerImpl->destroyTransactionEnv();
        }

        TransactionSendResult sendMessageInTransaction(const Message& msg,
                LocalTransactionExecuter* tranExecuter, void* arg)
        {
            if (NULL == m_pTransactionCheckListener)
            {
                THROW_MQEXCEPTION("localTransactionBranchCheckListener is null", -1);
            }

            return m_pDefaultMQProducerImpl.sendMessageInTransaction(msg, tranExecuter, arg);
        }

        TransactionCheckListener* getTransactionCheckListener()
        {
            return m_pTransactionCheckListener;
        }

        void setTransactionCheckListener(TransactionCheckListener* pTransactionCheckListener)
        {
            m_pTransactionCheckListener = pTransactionCheckListener;
        }

        int getCheckThreadPoolMinSize()
        {
            return m_checkThreadPoolMinSize;
        }

        void setCheckThreadPoolMinSize(int checkThreadPoolMinSize)
        {
            m_checkThreadPoolMinSize = checkThreadPoolMinSize;
        }

        int getCheckThreadPoolMaxSize()
        {
            return m_checkThreadPoolMaxSize;
        }

        void setCheckThreadPoolMaxSize(int checkThreadPoolMaxSize)
        {
            m_checkThreadPoolMaxSize = checkThreadPoolMaxSize;
        }

        int getCheckRequestHoldMax()
        {
            return m_checkRequestHoldMax;
        }

        void setCheckRequestHoldMax(int checkRequestHoldMax)
        {
            m_checkRequestHoldMax = checkRequestHoldMax;
        }

    private:
        TransactionCheckListener* m_pTransactionCheckListener;
        int m_checkThreadPoolMinSize;
        int m_checkThreadPoolMaxSize;
        int m_checkRequestHoldMax;
    };
}

#endif
