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

#include "MixAll.h"
#include "FileUtil.h"

namespace rmq
{

const std::string MixAll::DEFAULT_TOPIC = "TBW102";
const std::string MixAll::BENCHMARK_TOPIC = "BenchmarkTest";
const std::string MixAll::DEFAULT_PRODUCER_GROUP = "DEFAULT_PRODUCER";
const std::string MixAll::DEFAULT_CONSUMER_GROUP = "DEFAULT_CONSUMER";
const std::string MixAll::TOOLS_CONSUMER_GROUP = "TOOLS_CONSUMER";
const std::string MixAll::CLIENT_INNER_PRODUCER_GROUP = "CLIENT_INNER_PRODUCER";
const std::string MixAll::SELF_TEST_TOPIC = "SELF_TEST_TOPIC";
const std::string MixAll::RETRY_GROUP_TOPIC_PREFIX = "%RETRY%";
const std::string MixAll::DLQ_GROUP_TOPIC_PREFIX = "%DLQ%";
const std::string MixAll::NAMESRV_ADDR_ENV = "NAMESRV_ADDR";
const std::string MixAll::ROCKETMQ_HOME_ENV = "ROCKETMQ_HOME";
const std::string MixAll::ROCKETMQ_HOME_PROPERTY = "rocketmq.home.dir";
const std::string MixAll::MESSAGE_COMPRESS_LEVEL = "rocketmq.message.compressLevel";
const std::string MixAll::ROCKETMQ_NAMESRV_DOMAIN = "172.30.30.125";

std::string MixAll::getRetryTopic(const std::string& consumerGroup)
{
    return RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
}

bool MixAll::compareAndIncreaseOnly(kpr::AtomicLong& target, long long value)
{
    long long current = target.get();
    while (value > current)
    {
        long long tmp = target.getAndSet(current, value);

        if (tmp == current)
        {
            return true;
        }

        current = target.get();
    }

    return false;
}


std::string MixAll::file2String(const std::string& fileName)
{
    return kpr::FileUtil::load2str(fileName);
}

void MixAll::string2File(const std::string& fileName, const std::string& fileData)
{
    // write tmp file
    std::string tmpFile = fileName + ".tmp";
    kpr::FileUtil::save2file(tmpFile, fileData);

    // backup old file
    std::string bakFile = fileName + ".bak";
    std::string oldFileData = kpr::FileUtil::load2str(fileName);
    if (!oldFileData.empty())
    {
        kpr::FileUtil::save2file(bakFile, oldFileData);
    }

    // delete old file
    std::remove(fileName.c_str());

    // rename file
    std::rename(tmpFile.c_str(), fileName.c_str());
}

}
