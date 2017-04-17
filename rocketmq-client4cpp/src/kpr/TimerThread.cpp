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
#include "TimerThread.h"
#include "KPRUtil.h"
#include "ScopedLock.h"

namespace kpr
{
unsigned int TimerThread::s_nextTimerID = 0;

TimerThread::TimerThread(const char* name, unsigned int checklnterval)
    : kpr::Thread(name), m_closed(false), m_checkInterval(checklnterval)
{
}

TimerThread::~TimerThread()
{
}

void TimerThread::Run()
{
    unsigned long long lastCheckTime = KPRUtil::GetCurrentTimeMillis();
    unsigned long long currentCheckTime = lastCheckTime;

    while (!m_closed)
    {
        currentCheckTime = KPRUtil::GetCurrentTimeMillis();
        unsigned int elapse = (unsigned int)(currentCheckTime - lastCheckTime);

        std::list<TimerInfo> timeList;

        CheckTimeOut(elapse, timeList);

        if (!timeList.empty())
        {
            std::list<TimerInfo>::iterator it = timeList.begin();
            for (; it != timeList.end(); it++)
            {
            	try
            	{
                	it->pTimerHandler->OnTimeOut(it->id);
                }
                catch(...)
                {
                	RMQ_ERROR("TimerThread[%s] OnTimeOut exception", GetName());
                }
            }
        }

        unsigned long long checkEndTime = KPRUtil::GetCurrentTimeMillis();
        int sleepTime = m_checkInterval - (int)(checkEndTime - currentCheckTime);
        if (sleepTime < 0)
        {
            sleepTime = 0;
        }

        lastCheckTime = currentCheckTime;

        try
        {
            kpr::ScopedLock<kpr::Monitor> lock(*this);
            Wait(sleepTime);
        }
        catch (...)
        {
        }
    }
}

void TimerThread::Stop()
{
    m_closed = true;
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    Notify();
}

unsigned int TimerThread::RegisterTimer(unsigned int initialDelay, unsigned int elapse, TimerHandler* pHandler, bool persistent)
{
    TimerInfo info;
    info.elapse = elapse;
    info.outTime = elapse - initialDelay;
    info.pTimerHandler = pHandler;
    info.persistent = persistent;

    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    info.id = GetNextTimerID();
    m_timers[info.id] = info;

    return info.id;
}

bool TimerThread::UnRegisterTimer(unsigned int timerId)
{
    bool result = false;
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    std::map<unsigned int, TimerInfo>::iterator it = m_timers.find(timerId);
    if (it != m_timers.end())
    {
        m_timers.erase(it);
        result = true;
    }

    return result;
}

bool TimerThread::ResetTimer(unsigned int timerId)
{
    bool result = false;
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    std::map<unsigned int, TimerInfo>::iterator it = m_timers.find(timerId);
    if (it != m_timers.end())
    {
        if (it->second.persistent)
        {
            it->second.outTime = it->second.elapse;
        }
        else
        {
            it->second.outTime = 0;
        }

        result = true;
    }

    return result;
}

bool TimerThread::CheckTimeOut(unsigned int elapse, std::list<TimerInfo>& timerList)
{
    bool result = false;
    timerList.clear();

    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    if (!m_timers.empty())
    {
        std::map<unsigned int, TimerInfo>::iterator it = m_timers.begin();
        while (it != m_timers.end())
        {
            it->second.outTime += elapse;

            if (it->second.outTime >= int(it->second.elapse))
            {
                timerList.push_back(it->second);

                if (it->second.persistent)
                {
                    it->second.outTime = 0;
                    ++it;
                }
                else
                {
                    std::map<unsigned int, TimerInfo>::iterator it1 = it;
                    ++it;
                    m_timers.erase(it1);
                }
            }
            else
            {
                ++it;
            }
        }

        result = true;
    }

    return result;
}

unsigned int TimerThread::GetNextTimerID()
{
    return ++s_nextTimerID;
}
}
