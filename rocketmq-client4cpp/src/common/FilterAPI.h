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

#ifndef __FILTERAPI_H__
#define __FILTERAPI_H__

#include <string>
#include "SubscriptionData.h"
#include "UtilAll.h"
#include "MQClientException.h"

namespace rmq
{
    class FilterAPI
    {
    public:
        static SubscriptionData* buildSubscriptionData(const std::string topic, const std::string& subString)
        {
            SubscriptionData* subscriptionData = new SubscriptionData();
            subscriptionData->setTopic(topic);
            subscriptionData->setSubString(subString);

            if (subString.empty() || subString == SubscriptionData::SUB_ALL)
            {
                subscriptionData->setSubString(SubscriptionData::SUB_ALL);
            }
            else
            {
                std::vector<std::string> out;

                UtilAll::Split(out, subString, "||");

                if (out.empty())
                {
                    THROW_MQEXCEPTION(MQClientException, "FilterAPI subString split error", -1);
                }

                for (size_t i = 0; i < out.size(); i++)
                {
                    std::string tag = out[i];
                    if (!tag.empty())
                    {
                        std::string trimString = UtilAll::Trim(tag);

                        if (!trimString.empty())
                        {
                            subscriptionData->getTagsSet().insert(trimString);
                            subscriptionData->getCodeSet().insert(UtilAll::hashCode(trimString));
                        }
                    }
                }
            }

            return subscriptionData;
        }
    };
}

#endif
