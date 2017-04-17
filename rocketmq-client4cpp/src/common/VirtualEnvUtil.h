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

#ifndef __VIRTUALENVUTIL_H__
#define __VIRTUALENVUTIL_H__

#include <string>

namespace rmq
{
    /**
    * VirtualEnv API
    *
    * @author manhong.yqd<jodie.yqd@gmail.com>
    * @since 2013-8-26
    */
    class VirtualEnvUtil
    {
    public:
        static std::string buildWithProjectGroup(const std::string& origin, const std::string& projectGroup);
        static std::string clearProjectGroup(const std::string& origin, const std::string& projectGroup);

    public:
        static const char* VIRTUAL_APPGROUP_PREFIX;
    };
}

#endif
