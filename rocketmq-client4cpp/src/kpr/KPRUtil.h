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
#ifndef __KPR_UTIL_H__
#define __KPR_UTIL_H__

#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <stdlib.h>
#include <string>


class KPRUtil
{
public:
	static struct timespec CalcAbsTime(long timeout);
	static unsigned long long GetCurrentTimeMillis();
	static long long str2ll(const char* str);
    static std::string lower(const std::string& s);
    static std::string upper(const std::string& s);
};


#endif
