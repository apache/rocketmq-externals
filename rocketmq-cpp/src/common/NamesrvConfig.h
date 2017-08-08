/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __NAMESRVCONFIG_H__
#define __NAMESRVCONFIG_H__

#include <stdlib.h>
#include <string>
namespace rocketmq {
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
