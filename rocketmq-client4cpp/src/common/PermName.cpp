/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
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

#include "PermName.h"

namespace rmq
{

int PermName::PERM_PRIORITY = 0x1 << 3;
int PermName::PERM_READ = 0x1 << 2;
int PermName::PERM_WRITE = 0x1 << 1;
int PermName::PERM_INHERIT = 0x1 << 0;

bool PermName::isReadable(int perm)
{
    return (perm & PERM_READ) == PERM_READ;
}

bool PermName::isWriteable(int perm)
{
    return (perm & PERM_WRITE) == PERM_WRITE;
}

bool PermName::isInherited(int perm)
{
    return (perm & PERM_INHERIT) == PERM_INHERIT;
}

std::string PermName::perm2String(int perm)
{
    std::string pm("---");
    if (isReadable(perm))
    {
        pm.replace(0, 1, "R");
    }

    if (isWriteable(perm))
    {
        pm.replace(1, 2, "W");
    }

    if (isInherited(perm))
    {
        pm.replace(2, 3, "X");
    }

    return pm;
}

}
