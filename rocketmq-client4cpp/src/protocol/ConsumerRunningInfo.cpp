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
#include "ConsumerRunningInfo.h"

namespace rmq
{

const std::string ConsumerRunningInfo::PROP_NAMESERVER_ADDR = "PROP_NAMESERVER_ADDR";
const std::string ConsumerRunningInfo::PROP_THREADPOOL_CORE_SIZE = "PROP_THREADPOOL_CORE_SIZE";
const std::string ConsumerRunningInfo::PROP_CONSUME_ORDERLY = "PROP_CONSUMEORDERLY";
const std::string ConsumerRunningInfo::PROP_CONSUME_TYPE = "PROP_CONSUME_TYPE";
const std::string ConsumerRunningInfo::PROP_CLIENT_VERSION = "PROP_CLIENT_VERSION";
const std::string ConsumerRunningInfo::PROP_CONSUMER_START_TIMESTAMP = "PROP_CONSUMER_START_TIMESTAMP";


ConsumerRunningInfo::ConsumerRunningInfo()
{
}

ConsumerRunningInfo::~ConsumerRunningInfo()
{
}

void ConsumerRunningInfo::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "}";
    outData = ss.str();
}


std::string ConsumerRunningInfo::formatString()
{
	std::string sb = "rocketmq-client4cpp not suppport this feature";

	/*
	// 1
	{
		sb.append("#Consumer Properties#\n");
		Iterator<Entry<Object, Object>> it = m_properties.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Object, Object> next = it.next();
			String item =
					String.format("%-40s: %s\n", next.getKey().toString(), next.getValue().toString());
			sb.append(item);
		}
	}

	// 2
	{
		sb.append("\n\n#Consumer Subscription#\n");

		Iterator<SubscriptionData> it = m_subscriptionSet.iterator();
		int i = 0;
		while (it.hasNext()) {
			SubscriptionData next = it.next();
			String item = String.format("%03d Topic: %-40s ClassFilter: %-8s SubExpression: %s\n", //
				++i,//
				next.getTopic(),//
				next.isClassFilterMode(),//
				next.getSubString());

			sb.append(item);
		}
	}

	// 3
	{
		sb.append("\n\n#Consumer Offset#\n");
		sb.append(String.format("%-32s  %-32s  %-4s  %-20s\n",//
			"#Topic",//
			"#Broker Name",//
			"#QID",//
			"#Consumer Offset"//
		));

		Iterator<Entry<MessageQueue, ProcessQueueInfo>> it = m_mqTable.entrySet().iterator();
		while (it.hasNext()) {
			Entry<MessageQueue, ProcessQueueInfo> next = it.next();
			String item = String.format("%-32s  %-32s  %-4d  %-20d\n",//
				next.getKey().getTopic(),//
				next.getKey().getBrokerName(),//
				next.getKey().getQueueId(),//
				next.getValue().getCommitOffset());

			sb.append(item);
		}
	}

	// 4
	{
		sb.append("\n\n#Consumer MQ Detail#\n");
		sb.append(String.format("%-32s  %-32s  %-4s  %-20s\n",//
			"#Topic",//
			"#Broker Name",//
			"#QID",//
			"#ProcessQueueInfo"//
		));

		Iterator<Entry<MessageQueue, ProcessQueueInfo>> it = m_mqTable.entrySet().iterator();
		while (it.hasNext()) {
			Entry<MessageQueue, ProcessQueueInfo> next = it.next();
			String item = String.format("%-32s  %-32s  %-4d  %s\n",//
				next.getKey().getTopic(),//
				next.getKey().getBrokerName(),//
				next.getKey().getQueueId(),//
				next.getValue().toString());

			sb.append(item);
		}
	}

	// 5
	{
		sb.append("\n\n#Consumer RT&TPS#\n");
		sb.append(String.format("%-32s  %14s %14s %14s %14s %18s %25s\n",//
			"#Topic",//
			"#Pull RT",//
			"#Pull TPS",//
			"#Consume RT",//
			"#ConsumeOK TPS",//
			"#ConsumeFailed TPS",//
			"#ConsumeFailedMsgsInHour"//
		));

		Iterator<Entry<String, ConsumeStatus>> it = m_statusTable.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ConsumeStatus> next = it.next();
			String item = String.format("%-32s  %14.2f %14.2f %14.2f %14.2f %18.2f %25d\n",//
				next.getKey(),//
				next.getValue().getPullRT(),//
				next.getValue().getPullTPS(),//
				next.getValue().getConsumeRT(),//
				next.getValue().getConsumeOKTPS(),//
				next.getValue().getConsumeFailedTPS(),//
				next.getValue().getConsumeFailedMsgs()//
				);

			sb.append(item);
		}
	}

	// 6
	if (m_jstack != null) {
		sb.append("\n\n#Consumer jstack#\n");
		sb.append(m_jstack);
	}
	*/

	return sb;
}


}
