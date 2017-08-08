/**
*@file Sync.h
*@brief the file to define Mutex class and ScopedLock class.
*
*@version 1.0.0
*@date 2008.12.22
*@author Bingbing Yang
*/
#ifndef _ALOG_SYNC_H
#define _ALOG_SYNC_H

#include <pthread.h>

namespace alog {

/**
*@class Mutex
*@brief enclosure the phread_mutex in this class.
*@version 1.0.0
*@date 2008.12.19
*@author Bingbing Yang
*@warning
*/
class Mutex
{
private:
    pthread_mutex_t mutex;

public:
    inline Mutex() {
        ::pthread_mutex_init(&mutex, NULL);
    }

    inline void lock() {
        ::pthread_mutex_lock(&mutex);
    }

    inline void unlock() {
        ::pthread_mutex_unlock(&mutex);
    }

    inline ~Mutex() {
        ::pthread_mutex_destroy(&mutex);
    }

private:
    Mutex(const Mutex& m);
    Mutex& operator=(const Mutex &m);
};

/**
*@class ScopedLock
*@brief simply make a scope lock by instant a ScopedLock object.
*@version 1.0.0
*@date 2008.12.19
*@author Bingbing Yang
*@warning
*/
class ScopedLock
{
private:
    Mutex& _mutex;

public:
    inline ScopedLock(Mutex& mutex) :
            _mutex(mutex) {
        _mutex.lock();
    }

    inline ~ScopedLock() {
        _mutex.unlock();
    }
};

/**
*@class RWMutex
*@brief enclosure the pthread_rwlock in this class to make a read-write locker.
*@version 1.0.0
*@date 2008.12.30
*@author Bingbing Yang
*@warning not use in this alog version yet considering the performance.
*/
class RWMutex
{
private:
    pthread_rwlock_t mutex;

public:
    inline RWMutex()
    {
        ::pthread_rwlock_init(&mutex, NULL);
    }

    inline void rdlock()
    {
        ::pthread_rwlock_rdlock(&mutex);
    }

    inline void wrlock()
    {
        ::pthread_rwlock_wrlock(&mutex);
    }

    inline void unlock()
    {
        ::pthread_rwlock_unlock(&mutex);
    }

    inline ~RWMutex()
    {
        ::pthread_rwlock_destroy(&mutex);
    }

private:
    RWMutex(const Mutex& m);
    RWMutex& operator=(const Mutex &m);
};

/**
*@class RScopedLock
*@brief simply make a read scope lock by instant a RScopedLock object.
*@version 1.0.0
*@date 2008.12.30
*@author Bingbing Yang
*@warning
*/
class RScopedLock
{
private:
    RWMutex& _mutex;

public:
    inline RScopedLock(RWMutex& mutex) :
            _mutex(mutex) {
        _mutex.rdlock();
    }

    inline ~RScopedLock() {
        _mutex.unlock();
    }
};

/**
*@class WScopedLock
*@brief simply make a write scope lock by instant a WScopedLock object.
*@version 1.0.0
*@date 2008.12.30
*@author Bingbing Yang
*@warning
*/
class WScopedLock
{
private:
    RWMutex& _mutex;

public:
    inline WScopedLock(RWMutex& mutex) :
            _mutex(mutex) {
        _mutex.wrlock();
    }

    inline ~WScopedLock() {
        _mutex.unlock();
    }
};

}
#endif
