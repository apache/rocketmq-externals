/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __VALIDATORST_H__
#define __VALIDATORST_H__

#include <string>
#include "MQClientException.h"
#include "MQMessage.h"
#include "UtilAll.h"
namespace metaq {
//<!***************************************************************************
class Validators {
 public:
  static bool regularExpressionMatcher(const string& origin,
                                       const string& patternStr);
  static string getGroupWithRegularExpression(const string& origin,
                                              const string& patternStr);
  static void checkTopic(const string& topic);
  static void checkGroup(const string& group);
  static void checkMessage(const MQMessage& msg, int maxMessageSize);

 public:
  static const string validPatternStr;
  static const int CHARACTER_MAX_LENGTH;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
