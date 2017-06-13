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

#include "ServiceThread.h"
#include "Monitor.h"
#include "ScopedLock.h"

namespace rmq
{

ServiceThread::ServiceThread(const char* name)
    : kpr::Thread(name),
      m_notified(false),
      m_stoped(false)
{

}

ServiceThread::~ServiceThread()
{

}

void ServiceThread::stop()
{
    m_stoped = true;
    wakeup();
}

void ServiceThread::wakeup()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);

    if (!m_notified)
    {
        m_notified = true;
        Notify();
    }
}

void ServiceThread::waitForRunning(long interval)
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    if (m_notified)
    {
        m_notified = false;
        return;
    }

    try
    {
        Wait(interval);
    }
    catch (...)
    {
        m_notified = false;
    }
}

}
