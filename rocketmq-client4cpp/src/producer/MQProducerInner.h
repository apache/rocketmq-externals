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

#ifndef __MQPRODUCERINNER_H__
#define __MQPRODUCERINNER_H__

#include <string>
#include <set>

namespace rmq
{
	class TransactionCheckListener;
	class MessageExt;
	class CheckTransactionStateRequestHeader;
	class TopicPublishInfo;

	class MQProducerInner
	{
	public:
	    virtual ~MQProducerInner() {}
	    virtual std::set<std::string> getPublishTopicList() = 0;
	    virtual bool isPublishTopicNeedUpdate(const std::string& topic) = 0;
	    virtual TransactionCheckListener* checkListener() = 0;
	    virtual void checkTransactionState(const std::string& addr, //
	                                       const MessageExt& msg, //
	                                       const CheckTransactionStateRequestHeader& checkRequestHeader) = 0;
	    virtual void updateTopicPublishInfo(const std::string& topic, TopicPublishInfo& info) = 0;
	};
}

#endif
