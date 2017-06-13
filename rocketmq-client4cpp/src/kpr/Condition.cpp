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
#include "Condition.h"

#include <errno.h>
#include <assert.h>

#include "Mutex.h"
#include "ScopedLock.h"
#include "Semaphore.h"
#include "KPRUtil.h"
#include "Exception.h"

namespace kpr
{
class ConditionHelper
{
    RecursiveMutex& m_mutex;
    int m_count;

public:

    ConditionHelper(RecursiveMutex& mutex, int count)
        : m_mutex(mutex),
          m_count(count)
    {
    }

    ~ConditionHelper()
    {
        pthread_mutex_unlock(&m_mutex.m_mutex);
        m_mutex.lock(m_count);
    }
};


Condition::Condition()
{
    pthread_cond_init(&m_cond, 0);
}

Condition::~Condition()
{
    pthread_cond_destroy(&m_cond);
}

void Condition::Wait(Mutex& mutex)
{
    wait(mutex, -1);
}

bool Condition::Wait(Mutex& mutex, long timeout)
{
    assert(timeout >= 0 && "timeout value is negative");

    return wait(mutex, timeout);
}

void Condition::Wait(RecursiveMutex& mutex)
{
    wait(mutex, -1);
}

bool Condition::Wait(RecursiveMutex& mutex, long timeout)
{
    assert(timeout >= 0 && "timeout value is negative");

    return wait(mutex, timeout);
}

void Condition::Notify()
{
    pthread_cond_signal(&m_cond);
}

void Condition::NotifyAll()
{
    pthread_cond_broadcast(&m_cond);
}

bool Condition::wait(Mutex& mutex, long timeout)
{
    int ret = 0;
    if (timeout < 0)
    {
        ret = pthread_cond_wait(&m_cond, &mutex.m_mutex);
    }
    else
    {
        struct timespec abstime = KPRUtil::CalcAbsTime(timeout);
        ret = pthread_cond_timedwait(&m_cond, &mutex.m_mutex, &abstime);
    }
    if (ret == 0)
    {
        return true;
    }
    else
    {
        if (errno == EINTR)
        {
            THROW_EXCEPTION(InterruptedException, "pthread_cond_timedwait failed", errno);
        }
        else if (errno == ETIMEDOUT && timeout >= 0)
        {
            return false;
        }
    }
    return true;
}

bool Condition::wait(RecursiveMutex& mutex, long timeout)
{
    unsigned int count = mutex.reset4Condvar();
    ConditionHelper unlock(mutex, count);

    int ret = 0;
    if (timeout < 0)
    {
        ret = pthread_cond_wait(&m_cond, &mutex.m_mutex);
    }
    else
    {
        struct timespec abstime = KPRUtil::CalcAbsTime(timeout);
        ret = pthread_cond_timedwait(&m_cond, &mutex.m_mutex, &abstime);
    }

    if (ret == 0)
    {
        return true;
    }
    else
    {
        if (errno == EINTR)
        {
            THROW_EXCEPTION(InterruptedException, "pthread_cond_timedwait failed", errno);
        }
        else if (errno == ETIMEDOUT && timeout >= 0)
        {
            return false;
        }
    }

    return true;
}
}
