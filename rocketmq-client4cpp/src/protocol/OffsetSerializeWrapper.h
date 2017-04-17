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

#ifndef __OFFSETSERIALIZEWRAPPER_H__
#define __OFFSETSERIALIZEWRAPPER_H__

#include <map>
#include <string>
#include "RemotingSerializable.h"
#include "MessageQueue.h"
#include "AtomicValue.h"
#include "UtilAll.h"
#include "json/json.h"


namespace rmq
{
    class OffsetSerializeWrapper : public RemotingSerializable
    {
    public:
        void encode(std::string& outData)
        {
            Json::Value offsetTable;
            RMQ_FOR_EACH(m_offsetTable, it)
            {
                MessageQueue mq = it->first;
                kpr::AtomicLong& offset = it->second;

                std::string mqStr = mq.toJsonString();
                offsetTable[mqStr] = offset.get();
            }

            Json::Value obj;
            obj["offsetTable"] = offsetTable;

            Json::FastWriter writer;
            outData = writer.write(obj);
        }
        static OffsetSerializeWrapper* decode(const char* pData, int len)
        {
            /*
            {
                "offsetTable":{
                    '{"brokerName":"broker-a","queueId":3,"topic":"TopicTest"}':0,
                    '{"brokerName":"broker-a","queueId":2,"topic":"TopicTest"}':0
                }

            }
            */

            RMQ_DEBUG("decode, data:%s", pData);

            Json::Reader reader;
            Json::Value obj;
            if (!reader.parse(pData, pData + len, obj))
            {
                return NULL;
            }

            RMQ_DEBUG("decode ok");

            if (obj.isObject())
            {
                Json::Value objOffsetTable = obj["offsetTable"];
                if (objOffsetTable.isObject())
                {
                    std::map<MessageQueue, kpr::AtomicLong> offsetTable;
                    OffsetSerializeWrapper* offsetWrapper = new OffsetSerializeWrapper();

                    Json::Value::Members members = objOffsetTable.getMemberNames();
                    for (typeof(members.begin()) it = members.begin(); it != members.end(); it++)
                    {
                        std::string key = *it;
                        Json::Value objMq;
                        RMQ_DEBUG("decode, key:%s", key.c_str());
                        if (!reader.parse(key, objMq))
                        {
                            continue;
                        }
                        RMQ_DEBUG("decode, key ok");

                        MessageQueue mq(objMq["topic"].asString(), objMq["brokerName"].asString(),
                                        objMq["queueId"].asInt());
                        long long offset = objOffsetTable[key].asInt64();

                        offsetTable[mq] = kpr::AtomicLong(offset);
                    }
                    offsetWrapper->setOffsetTable(offsetTable);

                    return offsetWrapper;
                }
            }

            return NULL;
        }

		std::string toString() const
		{
			std::stringstream ss;
			ss << "{offsetTable=" << UtilAll::toString(m_offsetTable)
			   << "}";
			return ss.str();
		}

        std::map<MessageQueue, kpr::AtomicLong>& getOffsetTable()
        {
            return m_offsetTable;
        }

        void setOffsetTable(const std::map<MessageQueue, kpr::AtomicLong>& table)
        {
            m_offsetTable = table;
        }

    private:
        std::map<MessageQueue, kpr::AtomicLong> m_offsetTable;
    };

    typedef kpr::RefHandleT<OffsetSerializeWrapper> OffsetSerializeWrapperPtr;
}

#endif
