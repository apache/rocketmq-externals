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

#ifndef __RESPONSEFUTURE_H__
#define __RESPONSEFUTURE_H__

#include <string>
#include "AtomicValue.h"
#include "RefHandle.h"

namespace kpr
{
class Monitor;
class Semaphore;
}

namespace rmq
{
    class InvokeCallback;
    class RemotingCommand;

    class ResponseFuture : public kpr::RefCount
    {
    public:
        ResponseFuture(int requestCode, int opaque, int timeoutMillis, InvokeCallback* pInvokeCallback,
			bool block, kpr::Semaphore* pSem);
        ~ResponseFuture();
        void executeInvokeCallback();
		void release();
        bool isTimeout();
        RemotingCommand* waitResponse(int timeoutMillis);
        void putResponse(RemotingCommand* pResponseCommand);
        long long getBeginTimestamp();
        bool isSendRequestOK();
        void setSendRequestOK(bool sendRequestOK);
        int getRequestCode();
        void setRequestCode(int requestCode);
        long long getTimeoutMillis();
        InvokeCallback* getInvokeCallback();
        RemotingCommand* getResponseCommand();
        void setResponseCommand(RemotingCommand* pResponseCommand);
        int getOpaque();
		std::string toString() const;

    private:
        RemotingCommand* m_pResponseCommand;
        volatile bool m_sendRequestOK;
        int m_requestCode;
        int m_opaque;
        long long m_timeoutMillis;
        InvokeCallback* m_pInvokeCallback;
        long long m_beginTimestamp;
        kpr::Monitor* m_pMonitor;
        bool m_notifyFlag;

        kpr::AtomicInteger m_exec;

		kpr::Semaphore* m_pSemaphore;
		kpr::AtomicInteger m_released;
    };
	typedef kpr::RefHandleT<ResponseFuture> ResponseFuturePtr;
}

#endif
