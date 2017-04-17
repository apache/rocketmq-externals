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
#include "AtomicValue.h"

#if !defined(__GCC_HAVE_SYNC_COMPARE_AND_SWAP_8)

#include "Mutex.h"

namespace kpr
{
static const size_t kSwapLockCount = 64;
static Mutex s_swapLocks[kSwapLockCount];

static inline Mutex& getSwapLock(const volatile int64_t* addr)
{
    return s_swapLocks[(reinterpret_cast<intptr_t>(addr) >> 3U) % kSwapLockCount];
}

static int64_t atomicAddAndFetch(int64_t volatile* ptr, int64_t step)
{
    Mutex& mutex = getSwapLock(ptr);

    mutex.Lock();
    int64_t value = *ptr + step;
    *ptr = value;
    mutex.Unlock();

    return value;
}

static int64_t atomicFetchAndAdd(int64_t volatile* ptr, int64_t step)
{
    Mutex& mutex = getSwapLock(ptr);

    mutex.Lock();
    int64_t value = *ptr;
    *ptr += step;
    mutex.Unlock();

    return value;
}

static bool atomicBoolCompareAndSwap(int64_t volatile* ptr, int64_t oldval, int64_t newval)
{
    Mutex& mutex = getSwapLock(ptr);

    mutex.Lock();
    if (*ptr == oldval)
    {
    	*ptr = newval;
    	mutex.Unlock();
    	return true;
    }

    mutex.Unlock();
    return false;
}

static int64_t atomicValCompareAndSwap(int64_t volatile* ptr, int64_t oldval, int64_t newval)
{
    Mutex& mutex = getSwapLock(ptr);

    mutex.Lock();
    int64_t value = *ptr;
    if (value == oldval)
    {
    	*ptr = newval;
    	mutex.Unlock();
    	return value;
    }

    mutex.Unlock();
    return value;
}


static int64_t atomicTestAndSet(int64_t volatile* ptr, int64_t val)
{
    Mutex& mutex = getSwapLock(ptr);

    mutex.Lock();
    int64_t value = *ptr;
    *ptr = val;
    mutex.Unlock();

    return value;
}



extern "C" {
int64_t __sync_add_and_fetch_8(int64_t volatile* ptr, int64_t value)
{
    return atomicAddAndFetch(ptr, value);
}

int64_t __sync_sub_and_fetch_8(int64_t volatile* ptr, int64_t value)
{
    return atomicAddAndFetch(ptr, -value);
}

int64_t __sync_fetch_and_add_8(int64_t volatile* ptr, int64_t value)
{
    return atomicFetchAndAdd(ptr, -value);
}

int64_t __sync_fetch_and_sub_8(int64_t volatile* ptr, int64_t value)
{
    return atomicFetchAndAdd(ptr, -value);
}

bool __sync_bool_compare_and_swap_8(volatile int64_t* ptr, int64_t oldval, int64_t newval)
{
    return atomicBoolCompareAndSwap(ptr, oldval, newval);
}

int64_t __sync_val_compare_and_swap_8(volatile int64_t* ptr, int64_t oldval, int64_t newval)
{
    return atomicValCompareAndSwap(ptr, oldval, newval);
}

bool __sync_lock_test_and_set_8(int64_t volatile* ptr, int64_t value)
{
    return atomicTestAndSet(ptr, value);
}


} // extern "C"

} // namespace kpr
#endif


