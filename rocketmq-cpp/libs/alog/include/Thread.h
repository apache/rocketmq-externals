/**
* @file     Thread.h
* @brief    Abstraction of the posix thread
*
* @version  1.0.0
* @date     2013.1.14
* @author   yuejun.huyj
*/

#ifndef __ALOG_THREAD_H__
#define __ALOG_THREAD_H__

#include <pthread.h>

namespace alog
{

typedef void (thread_fn) (void*);

//
// Class encapsulating OS thread. Thread initiation/termination is done
// using special functions rather than in constructor/destructor so that
// thread isn't created during object construction by accident, causing
// newly created thread to access half-initialised object. Same applies
// to the destruction process: Thread should be terminated before object
// destruction begins, otherwise it can access half-destructed object.
//
class Thread
{
public:

    Thread ()
    {
    }

    // Create OS thread. 'tfn' is main thread function. It'll be passed
    // 'arg' as an argument.
    void start(thread_fn *tfn, void *pArg);

    // Waits for thread termination.
    void stop();

    // These are internal members. They should be private, however then
    // they would not be accessible from the main routine of the thread.
    thread_fn *m_tfn;
    void *m_pArg;
    
private:

    pthread_t m_threadDescriptor;

    Thread(const Thread&);
    Thread &operator=(const Thread&);
};

}

#endif

