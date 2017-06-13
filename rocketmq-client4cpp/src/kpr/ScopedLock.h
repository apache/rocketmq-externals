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
#ifndef __KPR_SCOPEDLOCK_H__
#define __KPR_SCOPEDLOCK_H__

namespace kpr
{
template <class T>
class ScopedLock
{
public:
    ScopedLock(const T& mutex)
        : m_mutex(mutex)
    {
        m_mutex.Lock();
    }

    ~ScopedLock()
    {
        m_mutex.Unlock();
    }

private:
    const T& m_mutex;
};


template <class T>
class ScopedRLock
{
public:
    ScopedRLock(const T& mutex)
        : m_mutex(mutex)
    {
        m_mutex.ReadLock();
        m_acquired = true;
    }

    ~ScopedRLock()
    {
        if (m_acquired)
        {
            m_mutex.Unlock();
        }
    }

private:
    const T& m_mutex;
    mutable bool m_acquired;
};


template <class T>
class ScopedWLock
{
public:
    ScopedWLock(const T& mutex)
        : m_mutex(mutex)
    {
        m_mutex.WriteLock();
        m_acquired = true;
    }

    ~ScopedWLock()
    {
        if (m_acquired)
        {
            m_mutex.Unlock();
        }
    }

private:
    const T& m_mutex;
    mutable bool m_acquired;
};
}

#endif
