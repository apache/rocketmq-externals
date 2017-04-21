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
#include "Monitor.h"
#include <assert.h>

namespace kpr
{
Monitor::Monitor()
    : m_notifyCount(0)
{
}

Monitor::~Monitor()
{
}

void Monitor::Wait()
{
    validateOwner(m_mutex.GetOwner(), "wait()");

    notify(m_notifyCount);

    try
    {
        m_condition.Wait(m_mutex);
    }
    catch (...)
    {
        m_notifyCount = 0;
        throw;
    }
    m_notifyCount = 0;
}

void Monitor::Wait(long timeout)
{
    validateOwner(m_mutex.GetOwner(), "wait(long)");

    notify(m_notifyCount);
    try
    {
        m_condition.Wait(m_mutex, timeout);
    }
    catch (...)
    {
        m_notifyCount = 0;
        throw;
    }

    m_notifyCount = 0;
}

void Monitor::Notify()
{
    validateOwner(m_mutex.GetOwner(), "notify");

    if (m_notifyCount != -1)
    {
        ++m_notifyCount;
    }
}

void Monitor::NotifyAll()
{
    validateOwner(m_mutex.GetOwner(), "notifyAll");

    m_notifyCount = -1;
}

void Monitor::Lock() const
{
    if (m_mutex.Lock())
    {
        m_notifyCount = 0;
    }
}

void Monitor::Unlock() const
{
    if (m_mutex.GetCount() == 1)
    {
        ((Monitor*)this)->notify(m_notifyCount);
    }

    m_mutex.Unlock();
}

void Monitor::notify(int nnotify)
{
    if (nnotify != 0)
    {
        if (nnotify == -1)
        {
            m_condition.NotifyAll();
            return;
        }
        else
        {
            while (nnotify > 0)
            {
                m_condition.Notify();
                --nnotify;
            }
        }
    }
}

void Monitor::validateOwner(const ThreadId& id, const char* caller) const
{
    assert(id == ThreadId::GetCurrentThreadId());
}
}
