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
#ifndef __HEARTBEATDATA_H__
#define __HEARTBEATDATA_H__

#include <string>
#include <set>
#include <sstream>

#include "RocketMQClient.h"
#include "ConsumeType.h"
#include "SubscriptionData.h"
#include "RemotingSerializable.h"
#include "UtilAll.h"

namespace rmq
{
    struct ConsumerData
    {
        std::string groupName;
        ConsumeType consumeType;
        MessageModel messageModel;
        ConsumeFromWhere consumeFromWhere;
        std::set<SubscriptionData> subscriptionDataSet;
        bool operator < (const ConsumerData& cd)const
        {
            return groupName < cd.groupName;
        }

        void toJson(Json::Value& obj) const
        {
            //{"consumeFromWhere":"CONSUME_FROM_LAST_OFFSET","consumeType":"CONSUME_ACTIVELY","groupName":"please_rename_unique_group_name_5","messageModel":"CLUSTERING","subscriptionDataSet":[],"unitMode":false}
            obj["groupName"] = groupName;
            obj["messageModel"] = getMessageModelString(messageModel);
            obj["consumeFromWhere"] = getConsumeFromWhereString(consumeFromWhere);
            obj["consumeType"] = getConsumeTypeString(consumeType);
            obj["unitMode"] = false;

            Json::Value objSub(Json::arrayValue);
            RMQ_FOR_EACH(subscriptionDataSet, it)
            {
                Json::Value o;
                (*it).toJson(o);
                objSub.append(o);
            }
            obj["subscriptionDataSet"] = objSub;
        }

		std::string toString() const
        {
            std::stringstream ss;
            ss << "{groupName=" << groupName
               << ",messageModel=" << getMessageModelString(messageModel)
               << ",consumeFromWhere=" << getConsumeFromWhereString(consumeFromWhere)
               << ",consumeType=" << getConsumeTypeString(consumeType)
               << ",subscriptionDataSet=" << UtilAll::toString(subscriptionDataSet)
               << "}";
            return ss.str();
        }
    };
	inline std::ostream& operator<<(std::ostream& os, const ConsumerData& obj)
	{
	    os << obj.toString();
	    return os;
	}

    struct ProducerData
    {
        std::string groupName;
        bool operator < (const ProducerData& pd)const
        {
            return groupName < pd.groupName;
        }
        void toJson(Json::Value& obj) const
        {
            obj["groupName"] = groupName;
        }

		std::string toString() const
        {
            std::stringstream ss;
            ss << "{groupName=" << groupName << "}";
            return ss.str();
        }
    };
	inline std::ostream& operator<<(std::ostream& os, const ProducerData& obj)
	{
	    os << obj.toString();
	    return os;
	}


    class HeartbeatData : public RemotingSerializable
    {
    public:
        void encode(std::string& outData);

        std::string getClientID()
        {
            return m_clientID;
        }

        void setClientID(const std::string& clientID)
        {
            m_clientID = clientID;
        }

        std::set<ProducerData>& getProducerDataSet()
        {
            return m_producerDataSet;
        }

        void setProducerDataSet(const std::set<ProducerData>& producerDataSet)
        {
            m_producerDataSet = producerDataSet;
        }

        std::set<ConsumerData>& getConsumerDataSet()
        {
            return m_consumerDataSet;
        }

        void setConsumerDataSet(const std::set<ConsumerData>& consumerDataSet)
        {
            m_consumerDataSet = consumerDataSet;
        }

		std::string toString() const
		{
		    std::stringstream ss;
		    ss << "{clientID=" << m_clientID
		       << ",producerDataSet=" << UtilAll::toString(m_producerDataSet)
		       << ",consumerDataSet=" << UtilAll::toString(m_consumerDataSet) << "}";
		    return ss.str();
		}

    private:
        std::string m_clientID;
        std::set<ProducerData> m_producerDataSet;
        std::set<ConsumerData> m_consumerDataSet;
    };
}

#endif
