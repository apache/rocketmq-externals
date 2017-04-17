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

#ifndef __TOPICSTATSTABLE_H__
#define __TOPICSTATSTABLE_H__

#include <map>

namespace rmq
{
    class MessageQueue;

    typedef struct
    {
        long long minOffset;
        long long maxOffset;
        long long lastUpdateTimestamp;
    } TopicOffset;

    class TopicStatsTable
    {
    public:
        std::map<MessageQueue*, TopicOffset> getOffsetTable()
        {
            return m_offsetTable;
        }

        void setOffsetTable(const std::map<MessageQueue*, TopicOffset>& offsetTable)
        {
            m_offsetTable = offsetTable;
        }

    private:
        std::map<MessageQueue*, TopicOffset> m_offsetTable;
    };
}

#endif
