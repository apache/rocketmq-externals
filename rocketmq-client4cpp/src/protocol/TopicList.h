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
#ifndef __TOPICLIST_H__
#define __TOPICLIST_H__

#include <set>
#include <string>
#include <UtilAll.h>

namespace rmq
{
    class TopicList : public RemotingSerializable
    {
    public:
        static TopicList* decode(const char* pData, int len)
        {
            return new TopicList();
        }

        void encode(std::string& outData)
        {
        }

		std::string toString() const
		{
			std::stringstream ss;
			ss << "{topicList=" << UtilAll::toString(m_topicList)
			   << "}";
			return ss.str();
		}

        const std::set<std::string>& getTopicList()
        {
            return m_topicList;
        }

        void setTopicList(const std::set<std::string>& topicList)
        {
            m_topicList = topicList;
        }

    private:
        std::set<std::string> m_topicList;
    };
}

#endif
