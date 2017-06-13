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

#ifndef __MIXALL_H__
#define __MIXALL_H__

#include <string>
#include <vector>
#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <fnmatch.h>
#include <arpa/inet.h>
#include <ifaddrs.h>

#include "AtomicValue.h"

namespace rmq
{
    class MixAll
    {
    public:
        static const long MASTER_ID = 0L;
        static const std::string DEFAULT_TOPIC;
        static const std::string BENCHMARK_TOPIC;
        static const std::string DEFAULT_PRODUCER_GROUP;
        static const std::string DEFAULT_CONSUMER_GROUP;
        static const std::string TOOLS_CONSUMER_GROUP;
        static const std::string CLIENT_INNER_PRODUCER_GROUP;
        static const std::string SELF_TEST_TOPIC;
        static const std::string RETRY_GROUP_TOPIC_PREFIX;
        static const std::string DLQ_GROUP_TOPIC_PREFIX;
        static const std::string NAMESRV_ADDR_ENV;
        static const std::string ROCKETMQ_HOME_ENV;
        static const std::string  ROCKETMQ_HOME_PROPERTY;
        static const std::string MESSAGE_COMPRESS_LEVEL;
        static const std::string ROCKETMQ_NAMESRV_DOMAIN;

        static std::string getRetryTopic(const std::string& consumerGroup);
        static bool compareAndIncreaseOnly(kpr::AtomicLong& target, long long value);
        static std::string file2String(const std::string& fileName);
        static void string2File(const std::string& fileName, const std::string& fileData);
    };
}

#endif
