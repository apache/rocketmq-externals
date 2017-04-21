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
#ifndef __RMQ_ROCKETMQCLIENT_H__
#define __RMQ_ROCKETMQCLIENT_H__

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


class RocketMQUtil
{
public:
	enum
    {
        NONE_LOG    = 0,
        ERROR_LOG   = 1,
        WARN_LOG    = 2,
        INFO_LOG    = 3,
        DEBUG_LOG   = 4,
    };

public:
	static pid_t getPid();
	static pid_t getTid();

	static int getDiffDays(time_t tmFirst, time_t tmSecond);
	static std::string tm2str(const time_t &t, const std::string &sFormat);
	static std::string now2str(const std::string &sFormat);
	static std::string now2str();
	static int64_t getNowMs();
	static std::string str2fmt(const char* format, ...)__attribute__((format(__printf__,1,2)));

	static int initLog(const std::string& sLogPath);
	static void setLogLevel(int logLevel);
	static void writeLog(const char* fmt, ...) __attribute__((format(__printf__,1,2)));
	static inline bool isNeedLog(int level)
	{
		return (level <= _logLevel);
	};

public:
	static volatile int _logFd;
	static int _logLevel;
	static std::string _logPath;
};

#define RMQ_AUTO(name, value) typeof(value) name = value
#define RMQ_FOR_EACH(container, it) \
    for(typeof((container).begin()) it = (container).begin();it!=(container).end(); ++it)



#define RMQ_DEBUG(fmt, args...)	do{ if(RocketMQUtil::isNeedLog(RocketMQUtil::DEBUG_LOG)) RocketMQUtil::writeLog("%d-%d|[%s][%s:%s:%d][DEBUG]|"fmt"\n", RocketMQUtil::getPid(), RocketMQUtil::getTid(), RocketMQUtil::now2str().c_str(), __FILE__, __func__,__LINE__, ##args);}while(0)
#define RMQ_INFO(fmt, args...)	do{ if(RocketMQUtil::isNeedLog(RocketMQUtil::INFO_LOG))  RocketMQUtil::writeLog("%d-%d|[%s][%s:%s:%d][INFO]|"fmt"\n", RocketMQUtil::getPid(), RocketMQUtil::getTid(), RocketMQUtil::now2str().c_str(), __FILE__, __func__, __LINE__, ##args);}while(0)
#define RMQ_WARN(fmt, args...)	do{ if(RocketMQUtil::isNeedLog(RocketMQUtil::WARN_LOG))  RocketMQUtil::writeLog("%d-%d|[%s][%s:%s:%d][WARN]|"fmt"\n", RocketMQUtil::getPid(), RocketMQUtil::getTid(), RocketMQUtil::now2str().c_str(), __FILE__, __func__, __LINE__, ##args);}while(0)
#define RMQ_ERROR(fmt, args...)	do{ if(RocketMQUtil::isNeedLog(RocketMQUtil::ERROR_LOG)) RocketMQUtil::writeLog("%d-%d|[%s][%s:%s:%d][ERROR]|"fmt"\n", RocketMQUtil::getPid(), RocketMQUtil::getTid(), RocketMQUtil::now2str().c_str(), __FILE__, __func__, __LINE__, ##args);}while(0)

#define RMQ_PRINT(fmt, args...)	do{ printf("%d|[%s][%s:%s:%d][DEBUG]|"fmt"\n", RocketMQUtil::getTid(), RocketMQUtil::now2str().c_str(), __FILE__, __func__,__LINE__, ##args);}while(0)


#endif

