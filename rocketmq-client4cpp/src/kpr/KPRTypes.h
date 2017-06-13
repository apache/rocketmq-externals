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
#ifndef __KPR_TYPES_H__
#define __KPR_TYPES_H__

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/time.h>
#include <pthread.h>
#include <semaphore.h>


typedef pthread_key_t ThreadKey;


namespace kpr
{
class ThreadId
{
public:
    ThreadId(pthread_t id = 0)
        : m_threadId(id)
    {
    }

    bool operator==(const ThreadId& id) const
    {
        return m_threadId == id.m_threadId;
    }

    bool operator!=(const ThreadId& id) const
    {
        return !(*this == id);
    }

    operator pthread_t() const
    {
        return m_threadId;
    }

    static ThreadId GetCurrentThreadId()
    {
        return ThreadId(pthread_self());
    }

    pthread_t m_threadId;
};
}

#endif
