/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
#include "MQClientException.h"
#include "SubscriptionData.h"
#include "UtilAll.h"
namespace rocketmq {
//<!***************************************************************************
class FilterAPI {
 public:
  static SubscriptionData* buildSubscriptionData(const string topic,
                                                 const string& subString) {
    //<!delete in balance;
    SubscriptionData* subscriptionData = new SubscriptionData(topic, subString);

    if (subString.empty() || !subString.compare(SUB_ALL)) {
      subscriptionData->setSubString(SUB_ALL);
    } else {
      vector<string> out;
      UtilAll::Split(out, subString, "||");

      if (out.empty()) {
        THROW_MQEXCEPTION(MQClientException, "FilterAPI subString split error",
                          -1);
      }

      for (size_t i = 0; i < out.size(); i++) {
        string tag = out[i];
        if (!tag.empty()) {
          UtilAll::Trim(tag);
          if (!tag.empty()) {
            subscriptionData->putTagsSet(tag);
            subscriptionData->putCodeSet(tag);
          }
        }
      }
    }

    return subscriptionData;
  }
};

//<!***************************************************************************
}  //<!end namespace;
#endif
