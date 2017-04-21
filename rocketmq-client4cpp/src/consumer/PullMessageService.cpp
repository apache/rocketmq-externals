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

#include "PullMessageService.h"
#include <list>
#include "MQClientFactory.h"
#include "MQConsumerInner.h"
#include "PullRequest.h"
#include "DefaultMQPushConsumerImpl.h"
#include "ScopedLock.h"

namespace rmq
{

class SubmitPullRequestLater : public kpr::TimerHandler
{
public:
    SubmitPullRequestLater(PullMessageService* pService, PullRequest* pPullRequest)
        : m_pService(pService), m_pPullRequest(pPullRequest)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
        	m_pService->executePullRequestImmediately(m_pPullRequest);
        }
        catch(...)
        {
        	RMQ_ERROR("SubmitPullRequestLater OnTimeOut exception");
        }

        delete this;
    }

private:
    PullMessageService* m_pService;
    PullRequest* m_pPullRequest;
};


PullMessageService::PullMessageService(MQClientFactory* pMQClientFactory)
    : ServiceThread("PullMessageService"),
      m_pMQClientFactory(pMQClientFactory)
{
    m_TimerThread = new kpr::TimerThread("PullMessageService-timer", 10);
    m_TimerThread->Start();
}


PullMessageService::~PullMessageService()
{

}


void PullMessageService::executePullRequestLater(PullRequest* pPullRequest, long timeDelay)
{
    SubmitPullRequestLater* pHandler = new SubmitPullRequestLater(this, pPullRequest);
    m_TimerThread->RegisterTimer(0, timeDelay, pHandler, false);
}


void PullMessageService::executeTaskLater(kpr::TimerHandler* pHandler, long timeDelay)
{
    m_TimerThread->RegisterTimer(0, timeDelay, pHandler, false);
}


void PullMessageService::executePullRequestImmediately(PullRequest* pPullRequest)
{
    try
    {
        {
            kpr::ScopedLock<kpr::Mutex> lock(m_lock);
            m_pullRequestQueue.push_back(pPullRequest);
        }

        wakeup();
    }
    catch (...)
    {
    	RMQ_ERROR("executePullRequestImmediately pullRequestQueue.push");
    }
}

void PullMessageService::Run()
{
	RMQ_INFO("%s service started", getServiceName().c_str());

    while (!m_stoped)
    {
        try
        {
            bool wait = false;
            {
                kpr::ScopedLock<kpr::Mutex> lock(m_lock);
                if (m_pullRequestQueue.empty())
                {
                    wait = true;
                }
            }

            if (wait)
            {
                waitForRunning(5000);
            }

            PullRequest* pullRequest = NULL;
            {
                kpr::ScopedLock<kpr::Mutex> lock(m_lock);
                if (!m_pullRequestQueue.empty())
                {
                    pullRequest = m_pullRequestQueue.front();
                    m_pullRequestQueue.pop_front();
                }
            }

            if (pullRequest != NULL)
            {
                pullMessage(pullRequest);
            }
        }
        catch (...)
        {
			RMQ_ERROR("Pull Message Service Run Method exception");
        }
    }

    m_TimerThread->Stop();
    m_TimerThread->Join();

    RMQ_INFO("%s service end", getServiceName().c_str());
}

std::string PullMessageService::getServiceName()
{
    return "PullMessageService";
}


void PullMessageService::pullMessage(PullRequest* pPullRequest)
{
    MQConsumerInner* consumer = m_pMQClientFactory->selectConsumer(pPullRequest->getConsumerGroup());
    if (consumer != NULL)
    {
        DefaultMQPushConsumerImpl* impl = (DefaultMQPushConsumerImpl*) consumer;
        impl->pullMessage(pPullRequest);
    }
    else
    {
        RMQ_WARN("No matched consumer for the PullRequest {%s}, drop it", pPullRequest->toString().c_str());
    }
}

}
