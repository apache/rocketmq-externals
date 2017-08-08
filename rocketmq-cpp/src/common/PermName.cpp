/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "PermName.h"
#include "UtilAll.h"

namespace metaq {
//<!***************************************************************************
int PermName::PERM_PRIORITY = 0x1 << 3;
int PermName::PERM_READ = 0x1 << 2;
int PermName::PERM_WRITE = 0x1 << 1;
int PermName::PERM_INHERIT = 0x1 << 0;

bool PermName::isReadable(int perm) { return (perm & PERM_READ) == PERM_READ; }

bool PermName::isWriteable(int perm) {
  return (perm & PERM_WRITE) == PERM_WRITE;
}

bool PermName::isInherited(int perm) {
  return (perm & PERM_INHERIT) == PERM_INHERIT;
}

string PermName::perm2String(int perm) {
  string pm("---");
  if (isReadable(perm)) {
    pm.replace(0, 1, "R");
  }

  if (isWriteable(perm)) {
    pm.replace(1, 2, "W");
  }

  if (isInherited(perm)) {
    pm.replace(2, 3, "X");
  }

  return pm;
}

//<!***************************************************************************
}  //<!end namespace;
