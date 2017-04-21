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
#ifndef __TCPREQUESTPROCESSOR_H__
#define __TCPREQUESTPROCESSOR_H__

namespace rmq
{
	class RemotingCommand;
	class TcpTransport;

	class TcpRequestProcessor
	{
	public:
	    virtual ~TcpRequestProcessor() {}
	    virtual RemotingCommand* processRequest(TcpTransport* pTts, RemotingCommand* pRequest) = 0;
	};
}

#endif
