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
#ifndef __KPR_ATOMICVALUE_H__
#define __KPR_ATOMICVALUE_H__

#include "KPRTypes.h"

namespace kpr
{

template <class T>
class AtomicValue
{
public:
	AtomicValue()
		: value(0)
	{
	}

    AtomicValue(T init)
		: value(init)
	{
	}

    AtomicValue<T>& operator=(T newValue)
    {
        set(newValue);
        return *this;
    }

    AtomicValue<T>& operator=(const AtomicValue<T>& v)
    {
        set(v.get());

        return *this;
    }

    inline T operator+=(T n)
    {
        return __sync_add_and_fetch(&value, n);
    }

    inline T operator-=(T n)
    {
        return __sync_sub_and_fetch(&value, n);
    }

    inline T operator++()
    {
        return *this += 1;
    }

    inline T operator--()
    {
        return *this -= 1;
    }

    inline T fetchAndAdd(T n)
    {
        return __sync_fetch_and_add(&value, n);
    }

    inline T fetchAndSub(T n)
    {
        return __sync_fetch_and_sub(&value, n);
    }

    inline T operator++(int)
    {
        return fetchAndAdd(1);
    }

    inline T operator--(int)
    {
        return fetchAndSub(1);
    }

    operator T() const
    {
        return get();
    }

    T get() const
    {
        return const_cast<AtomicValue<T>*>(this)->fetchAndAdd(static_cast<T>(0));
    }

    void set(T n)
    {
        __sync_lock_test_and_set((T*)&value, n);
    }

    inline T getAndSet(T comparand, T exchange)
    {
        return __sync_val_compare_and_swap((T*)&value, comparand, exchange);
    }

	inline bool compareAndSet(T comparand, T exchange)
    {
        return __sync_bool_compare_and_swap((T*)&value, comparand, exchange);
    }

private:
    volatile T value;
};


template <class T>
class AtomicReference
{
public:
	AtomicReference() : value(NULL) {}
    AtomicReference(T* init) : value(init) {}

    AtomicReference<T>& operator=(T* newValue)
    {
        set(newValue);
        return *this;
    }

    AtomicReference<T>& operator=(const AtomicReference<T>& v)
    {
        set(v.get());

        return *this;
    }

	T* operator->() const
    {
        return get();
    }

    T& operator*()
    {
        return *get();
    }

    operator T*() const
    {
        return get();
    }

    T* get() const
    {
		if (value == NULL)
		{
			return NULL;
		}
		else
		{
        	return (T*)(__sync_fetch_and_add((uintptr_t*)&value, 0));
		}
    }

    void set(T* n)
    {
		if (value == NULL)
		{
			value = n;
		}
		else
		{
			__sync_lock_test_and_set((uintptr_t*)&value, n);
		}
    }

    inline T getAndSet(T* comparand, T* exchange)
    {
        return __sync_val_compare_and_swap((uintptr_t*)&value, comparand, exchange);
    }

	inline bool compareAndSet(T* comparand, T* exchange)
    {
        return __sync_bool_compare_and_swap((uintptr_t*)&value, comparand, exchange);
    }

private:
    volatile T* value;
};


typedef AtomicValue<bool> AtomicBoolean;
typedef AtomicValue<int> AtomicInteger;
typedef AtomicValue<long long> AtomicLong;

}
#endif
