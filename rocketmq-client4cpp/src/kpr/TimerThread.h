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
#ifndef __KPR_TIMERTHREAD_H__
#define __KPR_TIMERTHREAD_H__

#include <list>
#include <map>

#include "RocketMQClient.h"
#include "Thread.h"
#include "Mutex.h"
#include "Monitor.h"

namespace kpr
{
class TimerHandler
{
public:
    TimerHandler()
    {
    }

    virtual ~TimerHandler()
    {
    }

    virtual void OnTimeOut(unsigned int timerID) = 0;
};

typedef struct tagTimerlnfo
{
    unsigned int id;
    unsigned int elapse;
    int outTime;
    bool persistent;
    TimerHandler* pTimerHandler;
} TimerInfo;


class TimerThread : public kpr::Thread, public kpr::Monitor
{
public:
    TimerThread(const char* name, unsigned int checklnterval);
    virtual ~TimerThread();
    virtual void Run();
    virtual void Stop();

    virtual unsigned int RegisterTimer(unsigned int initialDelay, unsigned int elapse, TimerHandler* pHandler, bool persistent = true);
    virtual bool UnRegisterTimer(unsigned int timerId);
    virtual bool ResetTimer(unsigned int timerId);

private:
    bool CheckTimeOut(unsigned int elapse, std::list<TimerInfo>& timerList);
    static unsigned int GetNextTimerID();

private:
    static unsigned int s_nextTimerID;
    std::map<unsigned int, TimerInfo> m_timers;
    kpr::Mutex m_mutex;
    bool m_closed;
    unsigned int m_checkInterval;
};
typedef kpr::RefHandleT<TimerThread> TimerThreadPtr;
}

#endif
