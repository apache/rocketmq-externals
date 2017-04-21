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

#ifndef __TOPICCONFIG_H__
#define __TOPICCONFIG_H__

#include <string>
#include "TopicFilterType.h"

namespace rmq
    {
    /**
    * Topic
    *
    */
    class TopicConfig
    {
    public:
        TopicConfig();
        TopicConfig(const std::string& topicName);
        TopicConfig(const std::string& topicName, int readQueueNums, int writeQueueNums, int perm);
        ~TopicConfig();

        std::string encode();
        bool decode(const std::string& in);
        const std::string& getTopicName();
        void setTopicName(const std::string& topicName);
        int getReadQueueNums();
        void setReadQueueNums(int readQueueNums);
        int getWriteQueueNums();
        void setWriteQueueNums(int writeQueueNums);
        int getPerm();
        void setPerm(int perm);
        TopicFilterType getTopicFilterType();
        void setTopicFilterType(TopicFilterType topicFilterType);
		int getTopicSysFlag();
        void setTopicSysFlag(int topicSysFlag);
		bool isOrder();
        void setOrder(bool order);

    public:
        static int DefaultReadQueueNums;
        static int DefaultWriteQueueNums;

    private:
        static std::string SEPARATOR;

        std::string m_topicName;
        int m_readQueueNums;
        int m_writeQueueNums;
        int m_perm;
        TopicFilterType m_topicFilterType;
		int m_topicSysFlag;
		bool m_order;
    };
}

#endif
