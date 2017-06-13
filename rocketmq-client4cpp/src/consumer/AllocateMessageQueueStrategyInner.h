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
#ifndef __ALLOCATEMESSAGEQUEUESTRATEGYINNER_H__
#define __ALLOCATEMESSAGEQUEUESTRATEGYINNER_H__

#include <algorithm>

#include "AllocateMessageQueueStrategy.h"
#include "MQClientException.h"
#include "UtilAll.h"


namespace rmq
{

    class AllocateMessageQueueAveragely : public AllocateMessageQueueStrategy
    {
    public:
        virtual ~AllocateMessageQueueAveragely() {}
        virtual std::vector<MessageQueue>* allocate(
				const std::string& consumerGroup,
				const std::string& currentCID,
                std::vector<MessageQueue>& mqAll,
                std::list<std::string>& cidAll)
        {
            if (currentCID.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "currentCID is empty", -1);
            }

            if (mqAll.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "mqAll is empty", -1);
            }

            if (cidAll.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "cidAll is empty", -1);
            }

            int index = -1;
            int cidAllSize = cidAll.size();

            std::list<std::string>::iterator it = cidAll.begin();
            for (int i = 0; it != cidAll.end(); it++, i++)
            {
                if (*it == currentCID)
                {
                    index = i;
                    break;
                }
            }

            if (index == -1)
            {
				RMQ_ERROR("[BUG] ConsumerGroup: {%s} The consumerId: {%s} not in cidAll: {%s}", //
                    consumerGroup.c_str(),
                    currentCID.c_str(),
                    UtilAll::toString(cidAll).c_str());
                return NULL;
            }

            int mqAllSize = mqAll.size();
            int mod = mqAllSize % cidAllSize;
            int averageSize =
                mqAllSize <= cidAllSize ? 1 : (mod > 0 && index < mod ? mqAllSize / cidAllSize
                                               + 1 : mqAllSize / cidAllSize);
            int startIndex = (mod > 0 && index < mod) ? index * averageSize : index * averageSize + mod;

            std::vector<MessageQueue>* result = new std::vector<MessageQueue>();
            int range = std::min<int>(averageSize, mqAllSize - startIndex);

            for (int i = 0; i < range; i++)
            {
                result->push_back(mqAll.at((startIndex + i) % mqAllSize));
            }

            return result;
        }

        virtual std::string getName()
        {
            return "AVG";
        }
    };


    class AllocateMessageQueueAveragelyByCircle : public AllocateMessageQueueStrategy
    {
    public:
        virtual ~AllocateMessageQueueAveragelyByCircle() {}
        virtual std::vector<MessageQueue>* allocate(
				const std::string& consumerGroup,
				const std::string& currentCID,
                std::vector<MessageQueue>& mqAll,
                std::list<std::string>& cidAll)
        {
            if (currentCID.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "currentCID is empty", -1);
            }

            if (mqAll.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "mqAll is empty", -1);
            }

            if (cidAll.empty())
            {
                THROW_MQEXCEPTION(MQClientException, "cidAll is empty", -1);
            }

            int index = -1;
            std::list<std::string>::iterator it = cidAll.begin();
            for (int i = 0; it != cidAll.end(); it++, i++)
            {
                if (*it == currentCID)
                {
                    index = i;
                    break;
                }
            }

            if (index == -1)
            {
				RMQ_ERROR("[BUG] ConsumerGroup: {%s} The consumerId: {%s} not in cidAll: {%s}", //
                    consumerGroup.c_str(),
                    currentCID.c_str(),
                    UtilAll::toString(cidAll).c_str());
                return NULL;
            }

			std::vector<MessageQueue>* result = new std::vector<MessageQueue>();
	        for (int i = index; i < (int)mqAll.size(); i++)
			{
	            if (i % (int)cidAll.size() == index)
				{
					result->push_back(mqAll.at(i));
	            }
	        }

	        return result;
        }

        virtual std::string getName()
        {
            return "AVG_BY_CIRCLE";
        }
    };


    class AllocateMessageQueueByConfig : public AllocateMessageQueueStrategy
    {
    public:
        virtual ~AllocateMessageQueueByConfig() {}
        virtual std::vector<MessageQueue>* allocate(
				const std::string& consumerGroup,
				const std::string& currentCID,
                std::vector<MessageQueue>& mqAll,
                std::list<std::string>& cidAll)
        {
            return NULL;
        }

        virtual std::string getName()
        {
            return "CONFIG";
        }
    };


    class AllocateMessageQueueByMachineRoom : public AllocateMessageQueueStrategy
    {
    public:
        virtual ~AllocateMessageQueueByMachineRoom() {}
        virtual std::vector<MessageQueue>* allocate(
				const std::string& consumerGroup,
				const std::string& currentCID,
                std::vector<MessageQueue>& mqAll,
                std::list<std::string>& cidAll)
        {
            return NULL;
        }

        virtual std::string getName()
        {
            return "MACHINE_ROOM";
        }
    };
}

#endif
