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
#include "ThreadPool.h"

#include "RocketMQClient.h"
#include "ScopedLock.h"
#include "KPRUtil.h"

namespace kpr
{
ThreadPoolWorker:: ThreadPoolWorker(ThreadPool* pThreadPool, const char* strName)
    : kpr::Thread(strName),
      m_pThreadPool(pThreadPool),
      m_canWork(false),
      m_isWaiting(false),
      m_stop(false),
      m_idleTime(0),
      m_idle(true)
{

}

bool ThreadPoolWorker::IsIdle()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    return m_idle;
}

void ThreadPoolWorker:: SetIdle(bool idle)
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    m_idle = idle;
    m_idleTime = 0;
}

int ThreadPoolWorker::IdleTime(int idleTime)
{
    if (m_idle)
    {
        m_idleTime += idleTime;
    }
    else
    {
        m_idleTime = 0;
    }

    return m_idleTime;
}

void ThreadPoolWorker::Run()
{
    while (!m_stop)
    {
        SetIdle(true);
        {
            kpr::ScopedLock<kpr::Monitor> lock(*this);
            while (!m_canWork)
            {
                try
                {
                    m_isWaiting = true;
                    Wait();
                    m_isWaiting = false;
                }
                catch (...)
                {
                }
            }

            m_canWork = false;
        }

        while (!m_stop)
        {
            ThreadPoolWorkPtr request = m_pThreadPool->GetWork(this);
            if ((ThreadPoolWork*)(NULL) == request)
            {
                break;
            }

            SetIdle(false);

            try
            {
            	request->Do();
            }
            catch(...)
            {
            	RMQ_ERROR("thead[%s] doWork exception", GetName());
            }

			//delete request;
            request = NULL;
        }

        if (m_stop || m_pThreadPool->IsDestroy())
        {
            break;
        }
    }

    m_pThreadPool ->RemoveThread(this);
    m_pThreadPool = NULL;
}

void ThreadPoolWorker::WakeUp()
{
    SetIdle(false);
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    m_canWork = true;
    Notify();
}

void ThreadPoolWorker::Stop()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    m_canWork = true;
    m_stop = true;
    Notify();
}

bool ThreadPoolWorker:: IsWaiting()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    return m_isWaiting;
}

ThreadPool::ThreadPool(const char* name,
                        int count,
                        int minCount,
                        int maxCount,
                        int step,
                        int maxIdleTime,
                        int checkldleThreadsInterval)
{
    if (name == NULL)
    {
        snprintf(m_name, sizeof(m_name), "ThreadPool");
    }
    else
    {
        snprintf(m_name, sizeof(m_name), "%s", name);
    }

    m_destroy = false;
    m_minCount = minCount;
    m_maxCount = maxCount;
    m_maxIdleTime = maxIdleTime;
    m_count = 0;
    m_step = step;
    m_index = 0;

    m_lastRemoveIdleThreadsTime = KPRUtil::GetCurrentTimeMillis();

    if (m_minCount <= 0)
    {
        m_minCount = MIN_THREAD_COUNT;
    }

    if (m_maxCount < 0)
    {
        m_maxCount = MAX_THREAD_COUNT;
    }

    if (m_maxIdleTime < 0)
    {
        m_maxIdleTime = MAX_IDLE_THREAD_TIME;
    }

    if (m_maxCount != 0 && m_maxCount < m_minCount)
    {
        m_minCount = MIN_THREAD_COUNT;
    }

    if ((m_maxCount != 0 && count > m_maxCount) || count < m_minCount)
    {
        count = m_minCount;
    }

    if (checkldleThreadsInterval < 0)
    {
        checkldleThreadsInterval = CHECK_IDLE_THREADS_INTERVAL;
    }

    AddThreads(count);

	char manager_name[32];
	snprintf(manager_name, sizeof(manager_name), "%s-manager", m_name);
    m_manager = new ThreadPoolManage(manager_name, this, checkldleThreadsInterval);
    m_manager->Start();
}

ThreadPool::~ThreadPool()
{
    Destroy();
}

void ThreadPool::AddThreads(int count)
{
    char threadName[256];

    for (int i = 0; i < count; ++i)
    {
        snprintf(threadName, sizeof(threadName), "%s-Worker%d", m_name, m_index);

        try
        {
            ThreadPoolWorkerPtr worker = new ThreadPoolWorker(this, threadName);
            worker->Start();

            m_workers.push_back(worker);
            while (!worker->IsWaiting())
            {
                kpr::Thread::Sleep(0, 100000);
            }

            m_index++;
            m_count++;
        }
        catch (...)
        {
			RMQ_ERROR("ThreadPool thead[%s] new exception", threadName);
        }
    }
}

void ThreadPool::Destroy()
{
    std::list<ThreadPoolWorkerPtr> workers;
    {
        kpr::ScopedLock<kpr::Monitor> lock(*this);
        if (m_destroy)
        {
            return;
        }

        m_destroy = true;

        std::list<ThreadPoolWorkerPtr>::iterator iter;
        for (iter = m_workers.begin(); iter != m_workers.end(); iter++)
        {
            workers.push_back(*iter);
            (*iter)->Stop();
        }
    }

    m_manager->Stop();
    m_manager->Join();

    std::list<ThreadPoolWorkerPtr>::iterator itThread;
    for (itThread = workers.begin(); itThread != workers.end(); itThread++)
    {
        (*itThread)->Join();
    }
    m_works.clear();
}

int ThreadPool::AddWork(ThreadPoolWorkPtr pWork)
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    if (m_destroy)
    {
        return -1;
    }

    m_works.push_back(pWork);

    if (!WakeOneThread())
    {
        if (0 == m_maxCount || m_count < m_maxCount)
        {
            int step = m_step;

            if (0 < m_maxCount && m_count + m_step > m_maxCount)
            {
                step = m_maxCount - m_count;
            }

            AddThreads(step);
            WakeOneThread();
        }
    }

    return 0;
}

ThreadPoolWorkPtr ThreadPool::GetWork(ThreadPoolWorker* pWorker)
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    ThreadPoolWorkPtr result = NULL;

    if (!m_destroy && !m_works.empty())
    {
        result = m_works.front();
        m_works.pop_front();
    }

    return result;
}

bool ThreadPool::IsDestroy()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    return m_destroy;
}

void ThreadPool::RemoveThread(ThreadPoolWorker* workerThread)
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);

    std::list<ThreadPoolWorkerPtr>::iterator it = m_workers.begin();

    for (; it != m_workers.end(); it++)
    {
        if ((*it) == workerThread)
        {
            m_workers.erase(it);
            m_count--;
            break;
        }
    }
}

void ThreadPool::RemoveIdleThreads()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);

    if (m_maxIdleTime == 0)
    {
        return;
    }

    unsigned long long time = KPRUtil::GetCurrentTimeMillis();
    int interval = (int)(time - m_lastRemoveIdleThreadsTime);
    m_lastRemoveIdleThreadsTime = time;

    std::list<ThreadPoolWorkerPtr>::iterator it = m_workers.begin();
    int size = (int)m_workers.size();
    while (size > m_minCount && it != m_workers.end())
    {
        if ((*it)->IdleTime(interval) > m_maxIdleTime)
        {
            (*it)->Stop();
            size--;
        }

        it++;
    }
}

bool ThreadPool::WakeOneThread()
{
    std::list<ThreadPoolWorkerPtr>::iterator it = m_workers.begin();
    for (; it != m_workers.end(); it++)
    {
        if ((*it)->IsIdle())
        {
            (*it)->WakeUp();
            return true;
        }
    }

    return false;
}

ThreadPoolManage::ThreadPoolManage(const char* name, ThreadPool* pThreadPool, int checkldleThreadsInterval)
    : kpr::Thread(name),
      m_pThreadPool(pThreadPool),
      m_stop(false),
      m_checkIdleThreadsInterval(checkldleThreadsInterval)
{
}

ThreadPoolManage::~ThreadPoolManage()
{
}

void ThreadPoolManage::Stop()
{
    kpr::ScopedLock<kpr::Monitor> lock(*this);
    m_stop = true;
    Notify();
}

void ThreadPoolManage::Run()
{
    while (!m_stop)
    {
        {
            kpr::ScopedLock<kpr::Monitor> lock(*this);
            if (!m_stop)
            {
                Wait(m_checkIdleThreadsInterval);
            }

            if (m_stop)
            {
                break;
            }
        }

        m_pThreadPool->RemoveIdleThreads();
    }
}
}
