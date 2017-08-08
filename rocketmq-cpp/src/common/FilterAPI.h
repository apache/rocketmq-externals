/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __FILTERAPI_H__
#define __FILTERAPI_H__

#include <string>
#include "MQClientException.h"
#include "SubscriptionData.h"
#include "UtilAll.h"
namespace metaq {
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
