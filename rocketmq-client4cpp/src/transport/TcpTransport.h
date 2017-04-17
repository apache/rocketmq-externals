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
#ifndef __TCPTRANSPORT_H__
#define __TCPTRANSPORT_H__

#include <map>
#include <string>
#include <list>
#include "Mutex.h"
#include "SocketUtil.h"

namespace rmq
{
    const int CLIENT_STATE_UNINIT = 0;
    const int CLIENT_STATE_INITED = 1;
    const int CLIENT_STATE_DISCONNECT = 2;
    const int CLIENT_STATE_CONNECTED = 3;

    const int CLIENT_ERROR_SUCCESS = 0;
    const int CLIENT_ERROR_INIT = 1;
    const int CLIENT_ERROR_INVALID_URL = 2;
    const int CLIENT_ERROR_CONNECT = 3;
    const int CLIENT_ERROR_OOM = 4;

    class TcpTransport
    {
    public:
        TcpTransport(std::map<std::string, std::string>& config);
        ~TcpTransport();

        int connect(const std::string& serverAddr, int timeoutMillis);
        bool isConnected();
        void close();

        int sendData(const char* pBuffer, int len, int nTimeOut = -1);
		int recvData(std::list<std::string*>& dataList);

        SOCKET getSocket();
        std::string& getServerAddr();
		unsigned long long getLastSendRecvTime();

    private:
        int sendOneMsg(const char* pBuffer, int len, int nTimeout);
        int recvMsg();
		void processData(std::list<std::string*>& dataList);
        bool resizeBuf(int nNewSize);
        void tryShrink(int nMsgLen);
        static int getMsgSize(const char* pBuf);

    private:
        int m_sfd;
        int m_state;
    	char* m_pRecvBuf;
        int m_recvBufSize;
        int m_recvBufUsed;
        int m_shrinkMax;
        int m_shrinkCheckCnt;
        kpr::Mutex m_sendLock;
        kpr::Mutex m_recvLock;
        std::string m_serverAddr;
		long long m_lastSendRecvTime;
    };
}

#endif
