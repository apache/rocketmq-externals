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
#ifndef __SUBSCRIPTIONDATA_H__
#define __SUBSCRIPTIONDATA_H__

#include <string>
#include <set>

#include "RocketMQClient.h"
#include "RemotingSerializable.h"
#include "RefHandle.h"
#include "json/json.h"

namespace rmq
{
    class SubscriptionData : public kpr::RefCount
    {
    public:
        SubscriptionData();
        SubscriptionData(const std::string& topic, const std::string& subString);

        std::string getTopic()const;
        void setTopic(const std::string& topic);

        std::string getSubString();
        void setSubString(const std::string& subString);

        std::set<std::string>& getTagsSet();
        void setTagsSet(const std::set<std::string>& tagsSet);

        long long getSubVersion();
        void setSubVersion(long long subVersion);

        std::set<int>& getCodeSet();
        void setCodeSet(const std::set<int>& codeSet);

        int hashCode();
        void toJson(Json::Value& obj) const;
		std::string toString() const;

        bool operator==(const SubscriptionData& other);
        bool operator<(const SubscriptionData& other)const;

    public:
        static std::string SUB_ALL;

    private:
        std::string m_topic;
        std::string m_subString;
        std::set<std::string> m_tagsSet;
        std::set<int> m_codeSet;
        long long m_subVersion ;
    };
	typedef kpr::RefHandleT<SubscriptionData> SubscriptionDataPtr;

	inline std::ostream& operator<<(std::ostream& os, const SubscriptionData& obj)
	{
	    os << obj.toString();
	    return os;
	}
}

#endif
