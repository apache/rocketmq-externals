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
#include "HeartbeatData.h"

namespace rmq
{

void HeartbeatData::encode(std::string& outData)
{
    //{"clientID":"10.6.223.90@16164","consumerDataSet":[{"consumeFromWhere":"CONSUME_FROM_LAST_OFFSET","consumeType":"CONSUME_ACTIVELY","groupName":"please_rename_unique_group_name_5","messageModel":"CLUSTERING","subscriptionDataSet":[],"unitMode":false}],"producerDataSet":[{"groupName":"CLIENT_INNER_PRODUCER"}]}
    Json::Value obj;
    obj["clientID"] = m_clientID;

    Json::Value consumerDataSet(Json::arrayValue);
    for (typeof(m_consumerDataSet.begin()) it = m_consumerDataSet.begin(); it != m_consumerDataSet.end(); it++)
    {
        Json::Value o;
        (*it).toJson(o);
        consumerDataSet.append(o);
    }
    obj["consumerDataSet"] = consumerDataSet;

    Json::Value producerDataSet(Json::arrayValue);
    for (typeof(m_producerDataSet.begin()) it = m_producerDataSet.begin(); it != m_producerDataSet.end(); it++)
    {
        Json::Value o;
        it->toJson(o);
        producerDataSet.append(o);
    }
    obj["producerDataSet"] = producerDataSet;

    Json::FastWriter outer;
    outData = outer.write(obj);
}




}
