/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __KVTABLE_H__
#define __KVTABLE_H__
#include <map>
#include <string>
#include "RemotingSerializable.h"

namespace metaq {
//<!***************************************************************************
class KVTable : public RemotingSerializable {
 public:
  virtual ~KVTable() { m_table.clear(); }

  void Encode(string& outData) {}

  const map<string, string>& getTable() { return m_table; }

  void setTable(const map<string, string>& table) { m_table = table; }

 private:
  map<string, string> m_table;
};
}  //<!end namespace;

#endif
