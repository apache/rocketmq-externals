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
#ifndef __KPR_CONDITION_H__
#define __KPR_CONDITION_H__

#include "KPRTypes.h"

namespace kpr
{
class Mutex;
class RWMutex;
class RecursiveMutex;

class Condition
{
public:
    Condition();
    ~Condition();
    void Wait(Mutex& mutex);

    bool Wait(Mutex& mutex, long timeout);

    void Wait(RecursiveMutex& mutex);

    bool Wait(RecursiveMutex& mutex, long timeout);

    void Notify();

    void NotifyAll();

private:
    bool  wait(Mutex&, long timeout);
    bool  wait(RecursiveMutex&, long timeout);

    Condition(const Condition&);
    void operator=(const Condition&);

    pthread_cond_t m_cond;
};
}
#endif
