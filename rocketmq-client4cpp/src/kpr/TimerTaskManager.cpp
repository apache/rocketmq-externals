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
#include "TimerTaskManager.h"
#include "ThreadPool.h"
#include "ScopedLock.h"

namespace kpr
{
TimerTaskManager::TimerTaskManager()
{
}

TimerTaskManager::~TimerTaskManager()
{
}

int TimerTaskManager::Init(int maxThreadCount, int checklnteval)
{
    try
    {
        m_pThreadPool = new ThreadPool("TimerThreadPool", 5, 5, maxThreadCount);
        m_timerThread = new TimerThread("TimerThread", checklnteval);
        m_timerThread->Start();
    }
    catch (...)
    {
        return -1;
    }

    return 0;
}

unsigned int TimerTaskManager::RegisterTimer(unsigned int initialDelay, unsigned int elapse, TimerTaskPtr pTask)
{
    unsigned int id = m_timerThread->RegisterTimer(initialDelay, elapse, this, true);

    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    m_timerTasks[id] = pTask;

    return id;
}

bool TimerTaskManager::UnRegisterTimer(unsigned int timerId)
{
    bool ret = m_timerThread->UnRegisterTimer(timerId);

    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    m_timerTasks.erase(timerId);

    return ret;
}

bool TimerTaskManager::ResetTimer(unsigned int timerId)
{
    return m_timerThread->ResetTimer(timerId);
}

void TimerTaskManager::OnTimeOut(unsigned int timerId)
{
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    std::map<unsigned int, TimerTaskPtr>::iterator it = m_timerTasks.find(timerId);
    if (it != m_timerTasks.end())
    {
        if (!it->second->IsProcessing())
        {
            it->second->SetProcessing(true);
            m_pThreadPool->AddWork((it->second).ptr());
        }
    }
}

void TimerTaskManager::Stop()
{
    m_timerThread->Stop();
    m_timerThread->Join();
    m_pThreadPool->Destroy();
}
}
