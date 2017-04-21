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

#ifndef __RMQ_MQCLIENTEXCEPTION_H__
#define __RMQ_MQCLIENTEXCEPTION_H__

#include <string>
#include <ostream>
#include <sstream>
#include <exception>

#include "RocketMQClient.h"

namespace rmq
{
	class MQException : public std::exception
	{
	public:
		MQException(const std::string& msg, int error,const char* file,int line)throw()
			: m_error(error),m_line(line),m_file(file)
		{
			try
			{
				std::stringstream ss;
				ss << "[" << file << ":" << line <<"]|error: " << error << "|msg:" << msg;
				m_msg = ss.str();
			}
			catch (...)
			{
			}
		}

		virtual ~MQException()throw()
		{
		}

		const char* what() const throw()
		{
			return m_msg.c_str();
		}

		int GetError() const throw()
		{
			return m_error;
		}

		virtual const char* GetType() const throw()
		{
			return "MQException";
		}

	protected:
		int m_error;
		int m_line;
		std::string m_msg;
		std::string m_file;
	};

	inline std::ostream& operator<<(std::ostream& os, const MQException& e)
	{
		os <<"Type:"<<e.GetType() <<  e.what();
		return os;
	}

	#define DEFINE_MQCLIENTEXCEPTION(name, parent) \
	class name : public parent \
	{\
	public:\
		name(const std::string& msg, int error,const char* file,int line) throw ()\
		: parent(msg, error, file, line) {}\
		virtual const char* GetType() const throw()\
	{\
		return #name;\
	}\
	};

	DEFINE_MQCLIENTEXCEPTION(MQClientException, MQException)
	DEFINE_MQCLIENTEXCEPTION(MQBrokerException, MQException)
	DEFINE_MQCLIENTEXCEPTION(InterruptedException, MQException)
	DEFINE_MQCLIENTEXCEPTION(UnknownHostException, MQException)

	DEFINE_MQCLIENTEXCEPTION(RemotingException, MQException)
	DEFINE_MQCLIENTEXCEPTION(RemotingCommandException, RemotingException)
	DEFINE_MQCLIENTEXCEPTION(RemotingConnectException, RemotingException)
	DEFINE_MQCLIENTEXCEPTION(RemotingSendRequestException, RemotingException)
	DEFINE_MQCLIENTEXCEPTION(RemotingTimeoutException, RemotingException)
	DEFINE_MQCLIENTEXCEPTION(RemotingTooMuchRequestException, RemotingException)

	#define THROW_MQEXCEPTION(e,msg,err) throw e(msg,err,__FILE__,__LINE__)
}

#endif
