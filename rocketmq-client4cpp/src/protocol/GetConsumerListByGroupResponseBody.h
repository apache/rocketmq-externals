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

#ifndef __GETCONSUMERLISTBYGROUPRESPONSEBODY_H__
#define __GETCONSUMERLISTBYGROUPRESPONSEBODY_H__

#include <string>
#include <sstream>
#include <list>
#include "UtilAll.h"
#include "RemotingSerializable.h"

namespace rmq
{
    class GetConsumerListByGroupResponseBody : public RemotingSerializable
    {
    public:
        GetConsumerListByGroupResponseBody()
        {

        }

        ~GetConsumerListByGroupResponseBody()
        {

        }

        void encode(std::string& outData)
        {

        }

        static GetConsumerListByGroupResponseBody* decode(const char* pData, int len)
        {
            /*
            {"consumerIdList":["10.12.22.213@DEFAULT", "10.12.22.213@xxx"]}
            */
            //RMQ_DEBUG("GET_CONSUMER_LIST_BY_GROUP_VALUE:%s", pData);

            Json::Reader reader;
            Json::Value object;
            if (!reader.parse(pData, pData + len, object))
            {
                RMQ_ERROR("parse fail: %s", reader.getFormattedErrorMessages().c_str());
                return NULL;
            }

            GetConsumerListByGroupResponseBody* rsp =  new GetConsumerListByGroupResponseBody();
            Json::Value cidList = object["consumerIdList"];
            for (size_t i = 0; i < cidList.size(); i++)
            {
                Json::Value cid = cidList[i];
                if (cid != Json::Value::null)
                {
                    rsp->m_consumerIdList.push_back(cid.asString());
                }
            }

            return rsp;
        }

        std::list<std::string>& getConsumerIdList()
        {
            return m_consumerIdList;
        }

        void setConsumerIdList(const std::list<std::string>& consumerIdList)
        {
            m_consumerIdList = consumerIdList;
        }

        std::string toString() const
        {
            std::stringstream ss;
            ss << "{consumerIdList=" << UtilAll::toString(m_consumerIdList) << "}";
            return ss.str();
        }

    private:
        std::list<std::string> m_consumerIdList;
    };
}

#endif
