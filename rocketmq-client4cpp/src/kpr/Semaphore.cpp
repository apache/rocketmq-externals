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

#include "Semaphore.h"

#include <unistd.h>
#include <sys/time.h>
#include "KPRUtil.h"

namespace kpr
{
Semaphore::Semaphore(long initial_count)
{
    sem_init(&m_sem, 0, initial_count);
}

Semaphore::~Semaphore()
{
    sem_destroy(&m_sem);
}

int Semaphore::GetValue()
{
	int value = 0;
	int rc = sem_getvalue(&m_sem, &value);
	if (rc < 0)
	{
		return rc;
	}
	return value;
}

bool Semaphore::Wait()
{
    int rc;
    rc = sem_wait(&m_sem);
    return !rc;
}

bool Semaphore::Wait(long timeout)
{
    int rc;
    if (timeout < 0)
    {
        rc = sem_wait(&m_sem);
    }
    else
    {
        struct timespec abstime = KPRUtil::CalcAbsTime(timeout);
        rc = sem_timedwait(&m_sem, &abstime);
    }

    return !rc;
}

void Semaphore::Release(int count)
{
    sem_post(&m_sem);
}
}
