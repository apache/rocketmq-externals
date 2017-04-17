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
#ifndef __TOPADDRESSING_H__
#define  __TOPADDRESSING_H__

#include <string>
#include <sstream>
#include "SocketUtil.h"

namespace rmq
{
    class TopAddressing
    {
    public:
        TopAddressing()
			: m_nsAddr("")
        {
        }

		const std::string& getNsAddr()
        {
            return m_nsAddr;
        }

		void setNsAddr(std::string& nsAddr)
        {
           	m_nsAddr = nsAddr;
        }

        std::string fetchNSAddr()
        {

            return "";
        }

    private:
		std::string m_nsAddr;
    };
}

#endif
