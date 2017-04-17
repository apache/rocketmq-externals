/**
* Copyright (C) 2013 kangliqiang, kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
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

namespace rmq
{
/**
 * Name server Config
 *
 */
class NamesrvConfig
{
public:
    NamesrvConfig()
    {
        m_kvConfigPath = "";

        char* home = getenv(MixAll::ROCKETMQ_HOME_ENV.c_str());
        if (home)
        {
            m_rocketmqHome = home;
        }
        else
        {
            m_rocketmqHome = "";
        }
    }

    const std::string& getRocketmqHome()
    {
        return m_rocketmqHome;
    }

    void setRocketmqHome(const std::string& rocketmqHome)
    {
        m_rocketmqHome = rocketmqHome;
    }

    const std::string& getKvConfigPath()
    {
        return m_kvConfigPath;
    }

    void setKvConfigPath(const std::string& kvConfigPath)
    {
        m_kvConfigPath = kvConfigPath;
    }

private:
    std::string m_rocketmqHome;
    std::string m_kvConfigPath;
};
}

#endif
