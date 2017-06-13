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
#ifndef __RMQ_SENDRESULT_H__
#define __RMQ_SENDRESULT_H__

#include "RocketMQClient.h"
#include "MessageQueue.h"

namespace rmq
{
	enum SendStatus
	{
		SEND_OK,
		FLUSH_DISK_TIMEOUT,
		FLUSH_SLAVE_TIMEOUT,
		SLAVE_NOT_AVAILABLE
	};

	/**
	* Send Message Result
	*
	*/
	class SendResult
	{
	public:
		SendResult();
		SendResult(const SendStatus& sendStatus,
			const std::string&  msgId,
			MessageQueue& messageQueue,
			long long queueOffset,
			std::string&  projectGroupPrefix);

		const std::string&  getMsgId();
		void setMsgId(const std::string&  msgId);
		SendStatus getSendStatus();
		void setSendStatus(const SendStatus& sendStatus);
		MessageQueue& getMessageQueue();
		void setMessageQueue(MessageQueue& messageQueue);
		long long getQueueOffset();
		void setQueueOffset(long long queueOffset);
		bool hasResult();

		std::string toString() const;
		std::string toJsonString() const;

	private:
		SendStatus m_sendStatus;
		std::string m_msgId;
		MessageQueue m_messageQueue;
		long long m_queueOffset;
	};

	enum LocalTransactionState
	{
		COMMIT_MESSAGE,
		ROLLBACK_MESSAGE,
		UNKNOW,
	};

	/**
	* Send transaction message result
	*
	*/
	class TransactionSendResult : public SendResult
	{
	public:
		TransactionSendResult();
		LocalTransactionState getLocalTransactionState();
		void setLocalTransactionState(LocalTransactionState localTransactionState);

	private:
		LocalTransactionState m_localTransactionState;
	};
}

#endif
