/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __NAMESRVCONFIG_H__
#define __NAMESRVCONFIG_H__

#include <stdlib.h>
#include <string>
namespace metaq {
//<!***************************************************************************
class NamesrvConfig {
 public:
  NamesrvConfig() {
    m_kvConfigPath = "";

    char* home = getenv(ROCKETMQ_HOME_ENV.c_str());
    if (home) {
      m_rocketmqHome = home;
    } else {
      m_rocketmqHome = "";
    }
  }

  const string& getRocketmqHome() const { return m_rocketmqHome; }

  void setRocketmqHome(const string& rocketmqHome) {
    m_rocketmqHome = rocketmqHome;
  }

  const string& getKvConfigPath() const { return m_kvConfigPath; }

  void setKvConfigPath(const string& kvConfigPath) {
    m_kvConfigPath = kvConfigPath;
  }

 private:
  string m_rocketmqHome;
  string m_kvConfigPath;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
