/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __QUERYRESULT_H__
#define __QUERYRESULT_H__

#include "MQMessageExt.h"
#include "RocketMQClient.h"

namespace metaq {
//<!************************************************************************
class ROCKETMQCLIENT_API QueryResult {
 public:
  QueryResult(uint64_t indexLastUpdateTimestamp,
              const vector<MQMessageExt*>& messageList) {
    m_indexLastUpdateTimestamp = indexLastUpdateTimestamp;
    m_messageList = messageList;
  }

  uint64_t getIndexLastUpdateTimestamp() { return m_indexLastUpdateTimestamp; }

  vector<MQMessageExt*>& getMessageList() { return m_messageList; }

 private:
  uint64_t m_indexLastUpdateTimestamp;
  vector<MQMessageExt*> m_messageList;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
