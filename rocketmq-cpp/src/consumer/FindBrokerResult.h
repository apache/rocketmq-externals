/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __FINDBROKERRESULT_H__
#define __FINDBROKERRESULT_H__

namespace metaq {
//<!************************************************************************
struct FindBrokerResult {
  FindBrokerResult(const std::string& sbrokerAddr, bool bslave)
      : brokerAddr(sbrokerAddr), slave(bslave) {}

 public:
  std::string brokerAddr;
  bool slave;
};
}

#endif
