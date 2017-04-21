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
#include "Thread.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include <signal.h>

#include "ScopedLock.h"
#include "Exception.h"

//for log
#include "RocketMQClient.h"

namespace kpr
{
kpr::AtomicInteger Thread::s_threadNumber = 0;

void* Thread::ThreadRoute(void* pArg)
{
    Thread* tv = ((Thread*)pArg);

    try
    {
        tv->Startup();
    }
    catch (...)
    {
    }

    try
    {
        tv->Cleanup();
    }
    catch (...)
    {
    }

    return 0;
}

Thread::Thread(const char* name)
{
    m_started = false;
    m_threadId = ThreadId();
    m_threadNumber = s_threadNumber++;

    SetName(name);
}

Thread::~Thread()
{
    try
    {
    }
    catch (...)
    {
    }
}

void Thread::SetName(const char* name)
{
    ScopedLock<Mutex> guard(m_mutex);

    if (name == NULL)
    {
        snprintf(m_name, sizeof(m_name), "Thread-%u", m_threadNumber);
    }
    else
    {
        snprintf(m_name, sizeof(m_name), "%s", name);
    }
}

const char* Thread::GetName() const
{
    ScopedLock<Mutex> guard(m_mutex);
    return m_name;
}

void Thread::Start()
{
    ScopedLock<Mutex> guard(m_mutex);

    if (m_started)
    {
        return;
    }

    pthread_attr_t attr;
    int retcode = 0;
    retcode = pthread_attr_init(&attr);
    if (retcode != 0)
    {
        THROW_EXCEPTION(SystemCallException, "pthread_attr_init failed!", errno)
    }

    pthread_t id;
    retcode = pthread_create(&id, &attr, ThreadRoute, (void*)this);
    if (retcode != 0)
    {
        THROW_EXCEPTION(SystemCallException, "pthread_create error", errno)
    }

    m_threadId = id;
    pthread_attr_destroy(&attr);
    m_started = true;
    RMQ_DEBUG("thread[%s][%ld] start successfully", m_name, (long)id);
}

void Thread::Run()
{
    //TODO support runable
}

bool Thread::IsAlive() const
{
    if (m_started)
    {
        int retcode = pthread_kill(m_threadId, 0);
        return (retcode == ESRCH);
    }

    return false;
}

void  Thread::Join()
{
    if (m_started)
    {
        pthread_join(m_threadId, NULL);
    }
}

void Thread::Sleep(long millis, int nanos)
{
    assert(millis >= 0 && nanos >= 0 && nanos < 999999);
    struct timespec tv;
    tv.tv_sec = millis / 1000;
    tv.tv_nsec = (millis % 1000) * 1000000 + nanos;
    nanosleep(&tv, 0);
}

void Thread::Yield()
{
    pthread_yield();
}

ThreadId Thread::GetId() const
{
    ScopedLock<Mutex> guard(m_mutex);
    return m_threadId;
}

void Thread::Startup()
{
    try
    {
    	RMQ_INFO("thread[%s] started", GetName());
        Run();
    }
    catch (...)
    {
    }
}

void Thread::Cleanup()
{
	RMQ_INFO("thread[%s] end", GetName());
}

}

