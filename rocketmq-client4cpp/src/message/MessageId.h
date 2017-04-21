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

#ifndef __MESSAGEID_H__
#define __MESSAGEID_H__

#include "SocketUtil.h"

namespace rmq
{
    class MessageId
    {
    public:
        MessageId(sockaddr address, long long offset)
            : m_address(address), m_offset(offset)
        {

        }

        sockaddr getAddress()
        {
            return m_address;
        }

        void setAddress(sockaddr address)
        {
            m_address = address;
        }

        long long getOffset()
        {
            return m_offset;
        }

        void setOffset(long long offset)
        {
            m_offset = offset;
        }

    private:
        sockaddr m_address;
        long long m_offset;
    };
}

#endif
