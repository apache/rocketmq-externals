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

#ifndef __CONSUMESTATS_H__
#define __CONSUMESTATS_H__

#include <map>
#include "MessageQueue.h"

namespace rmq
{
    typedef struct
    {
        long long brokerOffset;
        long long consumerOffset;
        long long lastTimestamp;
    } OffsetWrapper;

    /**
    * Consumer progress
    *
    * @author kangliqiang<kangliq@163.com>
    */
    class ConsumeStats
    {
    public:
        ConsumeStats()
            : m_consumeTps(0)
        {
        }

        ~ConsumeStats()
        {
        }

        long long computeTotalDiff()
        {

            long long diffTotal = 0L;

            //Iterator<Entry<MessageQueue, OffsetWrapper>> it = m_offsetTable.entrySet().iterator();
            //while (it.hasNext()) {
            //  Entry<MessageQueue, OffsetWrapper> next = it.next();
            //  long long diff = next.getValue().getBrokerOffset() - next.getValue().getConsumerOffset();
            //  diffTotal += diff;
            //}

            return diffTotal;
        }


        std::map<MessageQueue*, OffsetWrapper> getOffsetTable()
        {
            return m_offsetTable;
        }


        void setOffsetTable(const std::map<MessageQueue*, OffsetWrapper> offsetTable)
        {
            m_offsetTable = offsetTable;
        }


        long long getConsumeTps()
        {
            return m_consumeTps;
        }


        void setConsumeTps(long long consumeTps)
        {
            m_consumeTps = consumeTps;
        }

    private:

        std::map<MessageQueue*, OffsetWrapper> m_offsetTable;
        long long m_consumeTps;
    };
}

#endif
