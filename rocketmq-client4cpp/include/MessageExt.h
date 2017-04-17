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
#ifndef __RMQ_MESSAGEEXT_H__
#define __RMQ_MESSAGEEXT_H__

#include <sys/socket.h>
#include <string>
#include "Message.h"
#include "TopicFilterType.h"
#include "RocketMQClient.h"

namespace rmq
	{
	/**
	* Message extend
	*
	*/
	class MessageExt : public Message
	{
	public:
		MessageExt();

		MessageExt(int queueId,
				   long long bornTimestamp,
				   sockaddr bornHost,
				   long long storeTimestamp,
				   sockaddr storeHost,
				   std::string msgId);

		~MessageExt();

		static TopicFilterType parseTopicFilterType(int sysFlag);

		int getQueueId();
		void setQueueId(int queueId);

		long long getBornTimestamp();
		void setBornTimestamp(long long bornTimestamp);

		sockaddr getBornHost();
		std::string getBornHostString();
		std::string getBornHostNameString();
		void setBornHost(const sockaddr& bornHost);

		long long getStoreTimestamp();
		void setStoreTimestamp(long long storeTimestamp);

		sockaddr getStoreHost();
		std::string getStoreHostString();
		void setStoreHost(const sockaddr& storeHost);

		std::string getMsgId();
		void setMsgId(const std::string& msgId);

		int getSysFlag();
		void setSysFlag(int sysFlag);

		int getBodyCRC();
		void setBodyCRC(int bodyCRC);

		long long getQueueOffset();
		void setQueueOffset(long long queueOffset);

		long long getCommitLogOffset();
		void setCommitLogOffset(long long physicOffset);

		int getStoreSize();
		void setStoreSize(int storeSize);

		int getReconsumeTimes();
		void setReconsumeTimes(int reconsumeTimes);

		long long getPreparedTransactionOffset();
		void setPreparedTransactionOffset(long long preparedTransactionOffset);

		std::string toString() const;

	private:
		long long m_queueOffset;
		long long m_commitLogOffset;
		long long m_bornTimestamp;
		long long m_storeTimestamp;
		long long m_preparedTransactionOffset;
		int m_queueId;
		int m_storeSize;
		int m_sysFlag;
		int m_bodyCRC;
		int m_reconsumeTimes;
		sockaddr m_bornHost;
		sockaddr m_storeHost;
		std::string m_msgId;
	};
}

#endif
