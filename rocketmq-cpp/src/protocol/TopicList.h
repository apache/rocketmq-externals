/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __TOPICLIST_H__
#define __TOPICLIST_H__
#include <string>
#include <vector>
#include "dataBlock.h"

namespace metaq {
//<!***************************************************************************
class TopicList {
 public:
  static TopicList* Decode(const MemoryBlock* mem) { return new TopicList(); }

 private:
  vector<string> m_topicList;
};
//<!************************************************************************
}  //<!end namespace;

#endif
