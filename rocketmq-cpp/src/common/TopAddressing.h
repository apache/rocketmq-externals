/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __TOPADDRESSING_H__
#define __TOPADDRESSING_H__

#include <sys/time.h>
#include <boost/thread/thread.hpp>
#include <list>
#include <map>
#include <string>
#include "Logging.h"
#include "UtilAll.h"

namespace metaq {
class TopAddressing {
 public:
  TopAddressing(string unitName);
  virtual ~TopAddressing();

 public:
  string fetchNSAddr(const string& NSDomain);

 private:
  string clearNewLine(const string& str);
  void updateNameServerAddressList(const string& adds);
  int IsIPAddr(const char* sValue);

 private:
  boost::mutex m_addrLock;
  list<string> m_addrs;
  string m_unitName;
};
}
#endif
