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
#include "KPRUtil.h"
#include <assert.h>



unsigned long long KPRUtil::GetCurrentTimeMillis()
{
    struct timeval tv;
    gettimeofday(&tv, 0);
    return tv.tv_sec * 1000ULL + tv.tv_usec / 1000;
}

struct timespec KPRUtil::CalcAbsTime(long timeout)
{
    assert(timeout >= 0);
    struct timeval tv;
    gettimeofday(&tv, 0);

    struct timespec abstime;
    abstime.tv_sec = tv.tv_sec + (timeout / 1000);
    abstime.tv_nsec = (tv.tv_usec * 1000) + ((timeout % 1000) * 1000000);
    if (abstime.tv_nsec >= 1000000000)
    {
        ++abstime.tv_sec;
        abstime.tv_nsec -= 1000000000;
    }

    return abstime;
}

long long KPRUtil::str2ll(const char* str)
{
    return atoll(str);
}


std::string KPRUtil::lower(const std::string& s)
{
    std::string sString = s;
    for (std::string::iterator iter = sString.begin(); iter != sString.end(); ++iter)
    {
        *iter = tolower(*iter);
    }

    return sString;
}

std::string KPRUtil::upper(const std::string& s)
{
    std::string sString = s;

    for (std::string::iterator iter = sString.begin(); iter != sString.end(); ++iter)
    {
        *iter = toupper(*iter);
    }

    return sString;
}



