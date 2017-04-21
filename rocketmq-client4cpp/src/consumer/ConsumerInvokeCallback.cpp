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

#include "ConsumerInvokeCallback.h"
#include "ResponseFuture.h"
#include "PullResult.h"
#include "MQClientAPIImpl.h"
#include "PullCallback.h"
#include "MQClientException.h"
#include "RemotingCommand.h"

namespace rmq
{

ConsumerInvokeCallback::ConsumerInvokeCallback(PullCallback* pPullCallback, MQClientAPIImpl* pMQClientAPIImpl)
    : m_pPullCallback(pPullCallback),
      m_pMQClientAPIImpl(pMQClientAPIImpl)
{
}

ConsumerInvokeCallback::~ConsumerInvokeCallback()
{
    if (m_pPullCallback != NULL)
    {
        delete m_pPullCallback;
        m_pPullCallback = NULL;
    }
}

void ConsumerInvokeCallback::operationComplete(ResponseFuturePtr pResponseFuture)
{
    if (m_pPullCallback == NULL)
    {
        delete this;
        return;
    }

    RemotingCommand* response = pResponseFuture->getResponseCommand();
    if (response != NULL)
    {
        try
        {
            PullResult* pullResult = m_pMQClientAPIImpl->processPullResponse(response);
            response->setBody(NULL, 0, false);

            m_pPullCallback->onSuccess(*pullResult);

            pullResult->msgFoundList.clear();
            delete pullResult;
        }
        catch (MQException& e)
        {
            m_pPullCallback->onException(e);
        }

        delete response;
    }
    else
    {
        if (!pResponseFuture->isSendRequestOK())
        {
            std::string msg = "send request failed";
            MQClientException e(msg, -1, __FILE__, __LINE__);
            m_pPullCallback->onException(e);
        }
        else if (pResponseFuture->isTimeout())
        {
            std::string msg = "wait response timeout";
            MQClientException e(msg, -1, __FILE__, __LINE__);
            m_pPullCallback->onException(e);
        }
        else
        {
            std::string msg = "unknow reseaon";
            MQClientException e(msg, -1, __FILE__, __LINE__);
            m_pPullCallback->onException(e);
        }
    }

    delete this;
}

}
