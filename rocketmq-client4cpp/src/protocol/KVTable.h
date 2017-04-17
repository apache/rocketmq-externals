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

#ifndef __KVTABLE_H__
#define __KVTABLE_H__

#include <map>
#include <string>
#include "RemotingSerializable.h"
#include "UtilAll.h"

namespace rmq
{
    class KVTable : public RemotingSerializable
    {
    public:
        void encode(std::string& outData)
        {

        }

		std::string toString() const
		{
			std::stringstream ss;
			ss << "{table=" << UtilAll::toString(m_table)
			   << "}";
			return ss.str();
		}

        const std::map<std::string, std::string>& getTable()
        {
            return m_table;
        }

        void setTable(const std::map<std::string, std::string>& table)
        {
            m_table = table;
        }

    private:
        std::map<std::string, std::string> m_table ;
    };
}

#endif
