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
#ifndef __TOPICROUTEDATA_H__
#define __TOPICROUTEDATA_H__

#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <list>
#include <map>
#include <string>
#include <sstream>
#include "RocketMQClient.h"
#include "RemotingSerializable.h"
#include "UtilAll.h"
#include "MixAll.h"
#include "json/json.h"

namespace rmq
{
    struct QueueData
    {
        std::string brokerName;
        int readQueueNums;
        int writeQueueNums;
        int perm;

        bool operator < (const QueueData& other)
        {
            return brokerName < other.brokerName;
        }

        bool operator==(const QueueData& other)const
        {
            if (brokerName == other.brokerName
                && readQueueNums == other.readQueueNums
                && writeQueueNums == other.writeQueueNums
                && perm == other.perm)
            {
                return true;
            }

            return false;
        }

        std::string toString() const
        {
            std::stringstream ss;
            ss << "{brokerName=" << brokerName
               << ",readQueueNums=" << readQueueNums
               << ",writeQueueNums=" << writeQueueNums
               << ",perm=" << perm
               << "}";
            return ss.str();
        }
    };
    inline std::ostream& operator<<(std::ostream& os, const QueueData& obj)
    {
        os << obj.toString();
        return os;
    }


    struct BrokerData
    {
        std::string brokerName;
        std::map<int, std::string> brokerAddrs;

        bool operator < (const BrokerData& other)
        {
            return brokerName < other.brokerName;
        }

        bool operator == (const BrokerData& other)const
        {
            if (brokerName == other.brokerName
                && brokerAddrs == other.brokerAddrs)
            {
                return true;
            }

            return false;
        }

        std::string toString() const
        {
            std::stringstream ss;
            ss << "{brokerName=" << brokerName
               << ",brokerAddrs=" << UtilAll::toString(brokerAddrs)
               << "}";
            return ss.str();
        }
    };


    inline std::ostream& operator<<(std::ostream& os, const BrokerData& obj)
    {
        os << obj.toString();
        return os;
    }


    class TopicRouteData : public RemotingSerializable
    {
    public:
        void encode(std::string& outData)
        {

        }

        static TopicRouteData* encode(const char* pData, int len)
        {
            /*
            {
                "orderTopicConf":"",
                "brokerDatas":[
                    {"brokerAddrs":{0:"10.134.143.77:10911"},"brokerName":"broker-a"}
                ],
                "filterServerTable":{},
                "queueDatas":[
                    {"brokerName":"broker-a","perm":6,"readQueueNums":4,"topicSynFlag":0,"writeQueueNums":4}
                ]
            }
            */
            Json::Reader reader;
            Json::Value object;

            if (!reader.parse(pData, pData + len, object))
            {
                RMQ_ERROR("parse fail:%s", reader.getFormattedErrorMessages().c_str());
                return NULL;
            }

            TopicRouteData* trd = new TopicRouteData();
            trd->setOrderTopicConf(object["orderTopicConf"].asString());

            Json::Value qds = object["queueDatas"];
            for (size_t i = 0; i < qds.size(); i++)
            {
                QueueData d;
                Json::Value qd = qds[i];
                d.brokerName = qd["brokerName"].asString();
                d.readQueueNums = qd["readQueueNums"].asInt();
                d.writeQueueNums = qd["writeQueueNums"].asInt();
                d.perm = qd["perm"].asInt();

                trd->getQueueDatas().push_back(d);
            }

            Json::Value bds = object["brokerDatas"];
            for (size_t i = 0; i < bds.size(); i++)
            {
                BrokerData d;
                Json::Value bd = bds[i];
                d.brokerName = bd["brokerName"].asString();

                Json::Value bas = bd["brokerAddrs"];
                Json::Value::Members mbs = bas.getMemberNames();
                for (size_t i = 0; i < mbs.size(); i++)
                {
                    std::string key = mbs.at(i);
                    d.brokerAddrs[atoi(key.c_str())] = bas[key].asString();
                }

                trd->getBrokerDatas().push_back(d);
            }

            return trd;
        }

        static std::string selectBrokerAddr(BrokerData& data)
        {
            std::map<int, std::string>::iterator it = data.brokerAddrs.find(MixAll::MASTER_ID);
            std::string value = "";
            if (it == data.brokerAddrs.end())
            {
                it = data.brokerAddrs.begin();
                if (it != data.brokerAddrs.end())
                {
                    value = it->second;
                }
            }
            else
            {
                value = it->second;
            }

            return value;
        }

        std::list<QueueData>& getQueueDatas()
        {
            return m_queueDatas;
        }

        void setQueueDatas(const std::list<QueueData>& queueDatas)
        {
            m_queueDatas = queueDatas;
        }

        std::list<BrokerData>& getBrokerDatas()
        {
            return m_brokerDatas;
        }

        void setBrokerDatas(const std::list<BrokerData>& brokerDatas)
        {
            m_brokerDatas = brokerDatas;
        }

        const std::string& getOrderTopicConf()
        {
            return m_orderTopicConf;
        }

        void setOrderTopicConf(const std::string& orderTopicConf)
        {
            m_orderTopicConf = orderTopicConf;
        }

        bool operator ==(const TopicRouteData& other)
        {
            if (m_brokerDatas != other.m_brokerDatas)
            {
                return false;
            }

            if (m_orderTopicConf != other.m_orderTopicConf)
            {
                return false;
            }

            if (m_queueDatas != other.m_queueDatas)
            {
                return false;
            }

            return true;
        }

        std::string toString() const
        {
            std::stringstream ss;
            ss << "{orderTopicConf=" << m_orderTopicConf
               << ",queueDatas=" << UtilAll::toString(m_queueDatas)
               << ",brokerDatas=" << UtilAll::toString(m_brokerDatas)
               << "}";
            return ss.str();
        }

    private:
        std::string m_orderTopicConf;
        std::list<QueueData> m_queueDatas;
        std::list<BrokerData> m_brokerDatas;
    };
	typedef kpr::RefHandleT<TopicRouteData> TopicRouteDataPtr;

    inline std::ostream& operator<<(std::ostream& os, const TopicRouteData& obj)
    {
        os << obj.toString();
        return os;
    }

}

#endif
