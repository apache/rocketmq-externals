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
#ifndef __THREADPOOLWORK_H__
#define __THREADPOOLWORK_H__

#include "RefHandle.h"

namespace kpr
{

class ThreadPoolWork : public kpr::RefCount
{
public:
    virtual ~ThreadPoolWork() {}
    virtual void Do() = 0;
};
typedef kpr::RefHandleT<ThreadPoolWork> ThreadPoolWorkPtr;

}

#endif
