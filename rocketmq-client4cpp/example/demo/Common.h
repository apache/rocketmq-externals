/**
* Copyright (C) 2013 suwenkuang ,hooligan_520@qq.com
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

#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <assert.h>
#include <time.h>
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <pthread.h>

#include <sys/time.h>
#include <sys/timeb.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <sys/syscall.h>
#include <linux/unistd.h>

#include <cstdio>
#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include <map>
#include <set>


#define MYDEBUG(fmt, args...)   printf(fmt, ##args)
#define MYLOG(fmt, args...)     MyUtil::writelog("[%s]"fmt, RocketMQUtil::now2str().c_str(), ##args)

class MyUtil
{
public:
    static void msleep(long millis)
    {
        struct timespec tv;
        tv.tv_sec = millis / 1000;
        tv.tv_nsec = (millis % 1000) * 1000000;
        nanosleep(&tv, 0);
    }

    static long long str2ll( const char *str )
    {
        return atoll(str);
    }

    static unsigned long long getNowMs()
    {
        struct timeval tv;
        gettimeofday(&tv, 0);
        return tv.tv_sec * 1000ULL+tv.tv_usec/1000;
    }

    static int initLog(const std::string& logPath)
    {
        _logPath = logPath;
    }

    static void writelog(const char* fmt, ...)
    {
        if (_logPath.empty())
        {
            return;
        }

        static int logFd = -1;
        if (logFd < 0)
        {
            logFd = open(_logPath.c_str(), O_CREAT | O_RDWR | O_APPEND, 0666);
        }

        if (logFd > 0)
        {
            char buf[1024*128];
            buf[0] = buf[sizeof(buf) - 1] = '\0';

            va_list ap;
            va_start(ap, fmt);
            int size = vsnprintf(buf, sizeof(buf), fmt, ap);
            va_end(ap);

            write(logFd, buf, size);
        }

        return;
    }
public:
    static std::string _logPath;
};

/*
 * int test()
 * {
 *      TimeCount tc;
 *      tc.begin();
 *      func1();
 *      tc.end();
 *      cout << "cost:" << tc.countSec() << endl;
 * }
 */
class TimeCount
{
public:
    TimeCount()
    {
        m_tBegin.tv_sec  = 0;
        m_tBegin.tv_usec = 0;

        m_tEnd.tv_sec  = 0;
        m_tEnd.tv_usec = 0;
    }

    ~TimeCount(){}
public:
    void begin()
    {
        gettimeofday(&m_tBegin,0);
    }

    void end()
    {
        gettimeofday(&m_tEnd, 0);
    }

    int countMsec()
    {
        return (int)((m_tEnd.tv_sec - m_tBegin.tv_sec)*1000 + (m_tEnd.tv_usec -m_tBegin.tv_usec)/1000.0);
    }

    int countUsec()
    {
        return (m_tEnd.tv_sec - m_tBegin.tv_sec)*1000000+(m_tEnd.tv_usec -m_tBegin.tv_usec);
    }

    int countSec()
    {
        return (m_tEnd.tv_sec - m_tBegin.tv_sec);
    }

public:
    timeval m_tBegin;
    timeval m_tEnd;
};

