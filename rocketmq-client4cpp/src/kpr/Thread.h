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
#ifndef __KPR_THREAD_H__
#define __KPR_THREAD_H__

#include "KPRTypes.h"
#include "RefHandle.h"
#include "Mutex.h"

#ifdef Yield
#undef Yield
#endif

namespace kpr
{
class Thread : public virtual kpr::RefCount
{
public:
    Thread(const char* name = NULL);
    virtual ~Thread();

    virtual void Run();
    void Start();
    bool IsAlive() const;
    void Join();
    ThreadId   GetId() const;

    void SetName(const char*);
    const char* GetName() const;

    void Startup();
    void Cleanup();

    static void  Sleep(long millis, int nano = 0);
    static void  Yield();

private:
    Thread(const Thread&);
    const Thread& operator=(const Thread&);
    static void* ThreadRoute(void* pArg);

private:
    ThreadId m_threadId;
    unsigned int m_threadNumber;
    char m_name[128];
    bool m_started;
    Mutex m_mutex;

    static kpr::AtomicInteger s_threadNumber;
};
typedef kpr::RefHandleT<Thread> ThreadPtr;

}

#endif
