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
#ifndef __KPR_MONITOR_H__
#define __KPR_MONITOR_H__

#include "KPRTypes.h"
#include "Condition.h"
#include "Mutex.h"
namespace kpr
{
class Monitor
{
public:
    Monitor();
    virtual ~Monitor();

    void Wait();
    void Wait(long msec);

    void Notify();
    void NotifyAll();

    void Lock() const;
    void Unlock() const;

private:
    void notify(int times);
    void validateOwner(const ThreadId& id, const char* caller) const;

    RecursiveMutex m_mutex;
    Condition m_condition;
    mutable int m_notifyCount;
};
}
#endif
