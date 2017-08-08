/**
* @file     EventBase.h
* @brief    Deal with the asynchronous batch logging for FileAppenders
*
* @version  1.0.0
* @date     2013.1.12
* @author   yuejun.huyj
*/

#ifndef __EVENT_BASE_H__
#define __EVENT_BASE_H__

#include <cassert>
#include <vector>

#include "Sync.h"
#include "Appender.h"
#include "Thread.h"

namespace alog
{

class Condition {
public:
    Condition() {
        pthread_mutex_init(&m_mutex, NULL);
        pthread_cond_init(&m_cond, NULL);
    }

    ~Condition() {
        pthread_cond_destroy(&m_cond);
        pthread_mutex_destroy(&m_mutex);
    }

public:
    inline int lock() {
        return pthread_mutex_lock(&m_mutex);
    }

    inline int trylock () {
        return pthread_mutex_trylock(&m_mutex);
    }

    inline int unlock() {
        return pthread_mutex_unlock(&m_mutex);
    }

    inline int wait(int64_t ms)
    {
        timespec ts;
        ts.tv_sec = ms / 1000;
        ts.tv_nsec = (ms % 1000) * 1000000;
        return pthread_cond_timedwait(&m_cond, &m_mutex, &ts);
    }

    inline int signal() {
        return pthread_cond_signal(&m_cond);
    }

    inline int broadcast() {
        return pthread_cond_broadcast(&m_cond);
    }
private:
    pthread_mutex_t m_mutex;
    pthread_cond_t m_cond;
};
//
// EventBase handles the asynchronous signals sent from FileAppender. And it also
// deals with the timeout event of each FileAppender. Internally it uses epoll
// to capture the read and timeout events in a dedicated I/O thread.
// Note that: currently we only deal with the asynchronous events for *FileAppender*.
//
class EventBase
{
public:
    // Default flush time interval for each FileAppender.
    static const int64_t s_iDefaultFlushIntervalInMS = 1000; // 1s
    
    EventBase();
    ~EventBase();

    void addFileAppender(FileAppender *pFileAppender);
    void collectFileAppenders();
    bool isRunning() { return m_bRunning; }
    void notify();
    // stop the worker I/O thread 
    void stop();
    void flushFile(FileAppender* appender);

private:
    void wait(uint64_t usec);
    int64_t flush(bool force); 
    static void worker_routine(void *pArg);
    void loop();

private:
    Mutex m_pendingMutex;
    Mutex m_flushMutex;
    std::vector<FileAppender *> m_pendingFileAppenders;
    std::vector<FileAppender *> m_fileAppenders;
    Condition m_condition;
    volatile bool m_bRunning;
    volatile bool m_notified;
    Thread m_worker;
};

extern EventBase gEventBase;
}

#endif
