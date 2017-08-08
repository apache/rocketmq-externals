/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SUBSCRIPTIONDATA_H__
#define __SUBSCRIPTIONDATA_H__

#include <string>
#include "UtilAll.h"
#include "json/json.h"

namespace metaq {
//<!************************************************************************
class SubscriptionData {
 public:
  SubscriptionData();
  virtual ~SubscriptionData() {
    m_tagSet.clear();
    m_codeSet.clear();
  }
  SubscriptionData(const string& topic, const string& subString);
  SubscriptionData(const SubscriptionData& other);

  const string& getTopic() const;
  const string& getSubString() const;
  void setSubString(const string& sub);
  int64 getSubVersion() const;

  void putTagsSet(const string& tag);
  bool containTag(const string& tag);
  vector<string>& getTagsSet();

  void putCodeSet(const string& tag);

  bool operator==(const SubscriptionData& other) const;
  bool operator<(const SubscriptionData& other) const;

  MetaqJson::Value toJson() const;

 private:
  string m_topic;
  string m_subString;
  int64 m_subVersion;
  vector<string> m_tagSet;
  vector<int> m_codeSet;
};
//<!***************************************************************************
}  //<!end namespace;

#endif
