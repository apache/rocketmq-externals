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
#include "VirtualEnvUtil.h"

#include <stdlib.h>
#include <stdio.h>
#include "UtilAll.h"

namespace rmq
{

const char* VirtualEnvUtil::VIRTUAL_APPGROUP_PREFIX = "%%PROJECT_%s%%";

std::string VirtualEnvUtil::buildWithProjectGroup(const std::string& origin, const std::string& projectGroup)
{
    if (!UtilAll::isBlank(projectGroup))
    {
        char prefix[1024];
        snprintf(prefix, sizeof(prefix), VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());

        if (origin.find_last_of(prefix) == std::string::npos)
        {
            return origin + prefix;
        }
        else
        {
            return origin;
        }
    }
    else
    {
        return origin;
    }
}


std::string VirtualEnvUtil::clearProjectGroup(const std::string& origin, const std::string& projectGroup)
{
    char prefix[1024];
    snprintf(prefix, sizeof(prefix), VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());
    std::string::size_type pos = origin.find_last_of(prefix);

    if (!UtilAll::isBlank(prefix) && pos != std::string::npos)
    {
        return origin.substr(0, pos);
    }
    else
    {
        return origin;
    }
}

}
