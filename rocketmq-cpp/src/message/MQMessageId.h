/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGEID_H__
#define __MESSAGEID_H__

#include "SocketUtil.h"
#include "UtilAll.h"

namespace metaq {
//<!***************************************************************************
class MQMessageId {
 public:
  MQMessageId(sockaddr address, int64 offset)
      : m_address(address), m_offset(offset) {}

  sockaddr getAddress() const { return m_address; }

  void setAddress(sockaddr address) { m_address = address; }

  int64 getOffset() const { return m_offset; }

  void setOffset(int64 offset) { m_offset = offset; }

 private:
  sockaddr m_address;
  int64 m_offset;
};

}  //<!end namespace;

#endif
