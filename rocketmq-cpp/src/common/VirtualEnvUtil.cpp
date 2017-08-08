/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "VirtualEnvUtil.h"
#include <stdio.h>
#include <stdlib.h>
#include "UtilAll.h"

namespace metaq {
const char* VirtualEnvUtil::VIRTUAL_APPGROUP_PREFIX = "%%PROJECT_%s%%";

//<!***************************************************************************
string VirtualEnvUtil::buildWithProjectGroup(const string& origin,
                                             const string& projectGroup) {
  if (!UtilAll::isBlank(projectGroup)) {
    char prefix[1024];
    sprintf(prefix, VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());

    if (origin.find_last_of(prefix) == string::npos) {
      return origin + prefix;
    } else {
      return origin;
    }
  } else {
    return origin;
  }
}

string VirtualEnvUtil::clearProjectGroup(const string& origin,
                                         const string& projectGroup) {
  char prefix[1024];
  sprintf(prefix, VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());
  string::size_type pos = origin.find_last_of(prefix);

  if (!UtilAll::isBlank(prefix) && pos != string::npos) {
    return origin.substr(0, pos);
  } else {
    return origin;
  }
}

//<!***************************************************************************
}  //<!end namespace;
