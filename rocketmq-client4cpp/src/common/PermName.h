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

#ifndef __PERMNAME_H__
#define __PERMNAME_H__

#include <string>

namespace rmq
{
	class PermName
	{
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
}

#endif
