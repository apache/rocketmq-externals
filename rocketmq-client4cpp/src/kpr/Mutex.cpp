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
#include "Mutex.h"

#include <pthread.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <time.h>


namespace kpr
{
Mutex::Mutex()
{
    ::pthread_mutex_init(&m_mutex,  NULL);
}

Mutex::~Mutex()
{
    ::pthread_mutex_destroy(&m_mutex);
}

void Mutex::Lock() const
{
    ::pthread_mutex_lock(&m_mutex);
}

bool Mutex::TryLock() const
{
    int ret = ::pthread_mutex_trylock(&m_mutex);
    return (ret == 0);
}

bool Mutex::TryLock(int timeout) const
{
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
	ts.tv_sec += (timeout/1000);
	ts.tv_nsec += (timeout%1000) * 1000 * 1000;

    int ret = ::pthread_mutex_timedlock(&m_mutex, &ts);
    return (ret == 0);
}


void Mutex::Unlock() const
{
    ::pthread_mutex_unlock(&m_mutex);
}

//***********
//RWMutex
//***************
RWMutex::RWMutex()
{
    ::pthread_rwlock_init(&m_mutex,  NULL);
}

RWMutex::~RWMutex()
{
    ::pthread_rwlock_destroy(&m_mutex);
}

void RWMutex::ReadLock() const
{
    ::pthread_rwlock_rdlock(&m_mutex);
}

void RWMutex::WriteLock() const
{
    ::pthread_rwlock_wrlock(&m_mutex);
}

bool RWMutex::TryReadLock() const
{
    int ret = ::pthread_rwlock_tryrdlock(&m_mutex);
    return (ret == 0);
}

bool RWMutex::TryReadLock(int timeout) const
{
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
	ts.tv_sec += (timeout/1000);
	ts.tv_nsec += (timeout%1000) * 1000 * 1000;

    int ret = ::pthread_rwlock_timedrdlock(&m_mutex, &ts);
    return (ret == 0);
}

bool RWMutex::TryWriteLock() const
{
    int ret = ::pthread_rwlock_trywrlock(&m_mutex);
    return (ret == 0);
}

bool RWMutex::TryWriteLock(int timeout) const
{
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
	ts.tv_sec += (timeout/1000);
	ts.tv_nsec += (timeout%1000) * 1000 * 1000;

    int ret = ::pthread_rwlock_timedwrlock(&m_mutex, &ts);
    return (ret == 0);
}


void RWMutex::Unlock() const
{
    ::pthread_rwlock_unlock(&m_mutex);
}


//***********
//RecursiveMutex
//***************
RecursiveMutex::RecursiveMutex()
    : m_count(0),
      m_owner(ThreadId())
{
    ::pthread_mutex_init(&m_mutex,  NULL);
}

RecursiveMutex::~RecursiveMutex()
{
    ::pthread_mutex_destroy(&m_mutex);
}

bool RecursiveMutex::Lock()const
{
    return ((RecursiveMutex*)this)->lock(1);
}

bool RecursiveMutex::Unlock()const
{
    return ((RecursiveMutex*)this)->unlock();
}

bool RecursiveMutex::TryLock()const
{
    return ((RecursiveMutex*)this)->tryLock();
}

ThreadId RecursiveMutex::GetOwner()const
{
    m_internal.Lock();
    ThreadId id;
    if (m_count > 0)
    {
        id = m_owner;
    }
    m_internal.Unlock();

    return id;
}

bool RecursiveMutex::lock(int count)
{
    bool rc = false;
    bool obtained = false;

    while (!obtained)
    {
        m_internal.Lock();

        if (m_count == 0)
        {
            m_count = count;
            m_owner = ThreadId::GetCurrentThreadId();
            obtained = true;
            rc = true;

            try
            {
                ::pthread_mutex_lock(&m_mutex);
            }
            catch (...)
            {
                try
                {
                    m_internal.Unlock();
                }
                catch (...)
                {
                }
                throw;
            }
        }
        else if (m_owner == ThreadId::GetCurrentThreadId())
        {
            m_count += count;
            obtained = true;
        }

        m_internal.Unlock();

        if (!obtained)
        {
            ::pthread_mutex_lock(&m_mutex);
            ::pthread_mutex_unlock(&m_mutex);
        }
    }

    return rc;
}

bool RecursiveMutex::tryLock()
{
    bool obtained = false;

    m_internal.Lock();

    if (m_count == 0)
    {
        m_count = 1;
        m_owner = ThreadId::GetCurrentThreadId();
        obtained = true;

        try
        {
            ::pthread_mutex_lock(&m_mutex);
        }
        catch (...)
        {
            try
            {
                m_internal.Unlock();
            }
            catch (...)
            {
            }
            throw;
        }
    }
    else if (m_owner == ThreadId::GetCurrentThreadId())
    {
        ++m_count;
        obtained = true;
    }

    m_internal.Unlock();

    return obtained;
}

bool RecursiveMutex::unlock()
{
    bool rc;
    m_internal.Lock();

    if (--m_count == 0)
    {
        m_owner = ThreadId();

        ::pthread_mutex_unlock(&m_mutex);

        rc = true;
    }
    else
    {
        rc = false;
    }

    m_internal.Unlock();

    return rc;
}

unsigned int RecursiveMutex::reset4Condvar()
{
    m_internal.Lock();

    unsigned int count = m_count;
    m_count = 0;
    m_owner = ThreadId();

    m_internal.Unlock();

    return count;
}
}
