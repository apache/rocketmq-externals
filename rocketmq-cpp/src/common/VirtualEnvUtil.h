/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __VIRTUALENVUTIL_H__
#define __VIRTUALENVUTIL_H__

#include <string>
namespace metaq {
//<!***************************************************************************
class VirtualEnvUtil {
 public:
  static std::string buildWithProjectGroup(const std::string& origin,
                                      const std::string& projectGroup);
  static std::string clearProjectGroup(const std::string& origin,
                                  const std::string& projectGroup);

 public:
  static const char* VIRTUAL_APPGROUP_PREFIX;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
