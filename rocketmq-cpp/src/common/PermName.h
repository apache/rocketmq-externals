/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __PERMNAME_H__
#define __PERMNAME_H__

#include <string>

namespace metaq {
//<!***************************************************************************
class PermName {
 public:
  static int PERM_PRIORITY;
  static int PERM_READ;
  static int PERM_WRITE;
  static int PERM_INHERIT;

  static bool isReadable(int perm);
  static bool isWriteable(int perm);
  static bool isInherited(int perm);
  static std::string perm2String(int perm);
};

//<!***************************************************************************
}  //<!end namespace;
#endif
