/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __TOPICCONFIG_H__
#define __TOPICCONFIG_H__

#include <string>
#include "TopicFilterType.h"
#include "UtilAll.h"
namespace metaq {
//<!************************************************************************
class TopicConfig {
 public:
  TopicConfig();
  TopicConfig(const string& topicName);
  TopicConfig(const string& topicName, int readQueueNums, int writeQueueNums,
              int perm);
  ~TopicConfig();

  string encode();
  bool decode(const string& in);
  const string& getTopicName();
  void setTopicName(const string& topicName);
  int getReadQueueNums();
  void setReadQueueNums(int readQueueNums);
  int getWriteQueueNums();
  void setWriteQueueNums(int writeQueueNums);
  int getPerm();
  void setPerm(int perm);
  TopicFilterType getTopicFilterType();
  void setTopicFilterType(TopicFilterType topicFilterType);

 public:
  static int DefaultReadQueueNums;
  static int DefaultWriteQueueNums;

 private:
  static string SEPARATOR;

  string m_topicName;
  int m_readQueueNums;
  int m_writeQueueNums;
  int m_perm;
  TopicFilterType m_topicFilterType;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
