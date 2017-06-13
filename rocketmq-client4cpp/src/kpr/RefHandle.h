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
#ifndef __KPR_REFHANDLET_H__
#define __KPR_REFHANDLET_H__

#include "KPRTypes.h"
#include "AtomicValue.h"
#include "Exception.h"

namespace kpr
{

class RefCount
{
public:
	RefCount& operator=(const RefCount&)
	{
    	return *this;
	}

	void incRef()
	{
    	m_refCount++;
	}

	void decRef()
	{
	    if (--m_refCount == 0 && !m_noDelete)
	    {
	    	m_noDelete = true;
	        delete this;
	    }
	}

	int getRef() const
	{
		return m_refCount.get();
	}

	void setNoDelete(bool b)
	{
		m_noDelete = b;
	}

protected:
	RefCount()
    	: m_refCount(0), m_noDelete(false)
	{
	}

	RefCount(const RefCount&)
	    : m_refCount(0), m_noDelete(false)
	{
	}

	virtual ~RefCount()
	{
	}

protected:
	AtomicInteger m_refCount;
	bool		m_noDelete;
};



template <class T>
class RefHandleT
{
public:
    RefHandleT(T* p = 0)
    {
		m_ptr = p;

		if (m_ptr)
		{
			m_ptr->incRef();
		}
    }

	template<typename Y>
    RefHandleT(const RefHandleT<Y>& v)
    {
		m_ptr = v.m_ptr;

		if (m_ptr)
		{
			m_ptr->incRef();
		}
    }

	RefHandleT(const RefHandleT& v)
    {
		m_ptr = v.m_ptr;

		if (m_ptr)
		{
			m_ptr->incRef();
		}
    }

    ~RefHandleT()
    {
		if (m_ptr)
        {
            m_ptr->decRef();
        }
    }

	RefHandleT<T>& operator=(T* p)
    {
        if (m_ptr != p)
        {
			if (p)
            {
                p->incRef();
            }

			T* ptr = m_ptr;
            m_ptr = p;

            if (ptr)
            {
                ptr->decRef();
            }
        }

        return *this;
    }

	template<typename Y>
	RefHandleT<T>& operator=(const RefHandleT<Y>& v)
    {
        if (m_ptr != v.m_ptr)
        {
			if (v.m_ptr)
            {
                v.m_ptr->incRef();
            }

            T* ptr = m_ptr;
            m_ptr = v.m_ptr;

            if (ptr)
            {
                ptr->decRef();
            }
        }

        return *this;
    }

    RefHandleT<T>& operator=(const RefHandleT<T>& v)
    {
        if (m_ptr != v.m_ptr)
        {
			if (v.m_ptr)
            {
                v.m_ptr->incRef();
            }

            T* ptr = m_ptr;
            m_ptr = v.m_ptr;

            if (ptr)
            {
                ptr->decRef();
            }
        }

        return *this;
    }

    T* operator->() const
    {
		if (!m_ptr)
		{
			THROW_EXCEPTION(RefHandleNullException, "autoptr null handle error", -1);
		}

        return m_ptr;
    }

    T& operator*() const
    {
		if (!m_ptr)
		{
			THROW_EXCEPTION(RefHandleNullException, "autoptr null handle error", -1);
		}

        return *m_ptr;
    }

    operator T* () const
    {
        return m_ptr;
    }

    T* ptr() const
    {
        return m_ptr;
    }

    T* retn()
    {
        T* p = m_ptr;
        m_ptr = 0;

        return p;
    }

    bool operator==(const RefHandleT<T>& v) const
    {
        return m_ptr == v.m_ptr;
    }

    bool operator==(T* p) const
    {
        return m_ptr == p;
    }

    bool operator!=(const RefHandleT<T>& v) const
    {
        return m_ptr != v.m_ptr;
    }

    bool operator!=(T* p) const
    {
        return m_ptr != p;
    }

    bool operator!() const
    {
        return m_ptr == 0;
    }

    operator bool() const
    {
        return m_ptr != 0;
    }

    void swap(RefHandleT& other)
    {
        std::swap(m_ptr, other._ptr);
    }

	template<class Y>
    static RefHandleT dynamicCast(const RefHandleT<Y>& r)
    {
        return RefHandleT(dynamic_cast<T*>(r._ptr));
    }

    template<class Y>
    static RefHandleT dynamicCast(Y* p)
    {
        return RefHandleT(dynamic_cast<T*>(p));
    }

public:
    T* m_ptr;
};


template<typename T, typename U>
inline bool operator==(const RefHandleT<T>& lhs, const RefHandleT<U>& rhs)
{
    T* l = lhs.ptr();
    U* r = rhs.ptr();
    if(l && r)
    {
        return *l == *r;
    }
    else
    {
        return !l && !r;
    }
}


template<typename T, typename U>
inline bool operator!=(const RefHandleT<T>& lhs, const RefHandleT<U>& rhs)
{
    T* l = lhs.ptr();
    U* r = rhs.ptr();
    if(l && r)
    {
        return *l != *r;
    }
    else
    {
        return l || r;
    }
}


template<typename T, typename U>
inline bool operator<(const RefHandleT<T>& lhs, const RefHandleT<U>& rhs)
{
    T* l = lhs.ptr();
    U* r = rhs.ptr();
    if(l && r)
    {
        return *l < *r;
    }
    else
    {
        return !l && r;
    }
}

}

#define DECLAREVAR(T)   typedef kpr::RefHandleT<T> T ## Ptr;

#endif
