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
#ifndef __PULLMESSAGESERVICE_H__
#define __PULLMESSAGESERVICE_H__

#include <list>
#include "RocketMQClient.h"
#include "ServiceThread.h"
#include "TimerThread.h"
#include "PullRequest.h"

namespace rmq
{
    class MQClientFactory;
    class MQConsumerInner;
    class PullRequest;

    class PullMessageService : public ServiceThread
    {
    public:
        PullMessageService(MQClientFactory* pMQClientFactory);
        ~PullMessageService();

        void executePullRequestLater(PullRequest* pPullRequest, long timeDelay);
		void executeTaskLater(kpr::TimerHandler* pHandler, long timeDelay);

        void executePullRequestImmediately(PullRequest* pPullRequest);
        std::string getServiceName();

        void Run();
    private:
        void pullMessage(PullRequest* pPullRequest);

    private:
        std::list<PullRequest*> m_pullRequestQueue;
        kpr::Mutex m_lock;
        MQClientFactory* m_pMQClientFactory;
        kpr::TimerThreadPtr m_TimerThread;
    };
	typedef kpr::RefHandleT<PullMessageService> PullMessageServicePtr;
}

#endif
