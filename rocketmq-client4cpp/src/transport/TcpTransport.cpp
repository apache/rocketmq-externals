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
#include "TcpTransport.h"

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <errno.h>
#include <assert.h>
#include "KPRUtil.h"
#include "SocketUtil.h"
#include "Epoller.h"
#include "ScopedLock.h"

namespace rmq
{

const int DEFAULT_SHRINK_COUNT = 32;
const int DEFAULT_RECV_BUFFER_SIZE = 1024 * 16;

TcpTransport::TcpTransport(std::map<std::string, std::string>& config)
    : m_sfd(-1),
      m_state(CLIENT_STATE_UNINIT),
      m_pRecvBuf(NULL),
      m_recvBufSize(DEFAULT_RECV_BUFFER_SIZE),
      m_recvBufUsed(0),
      m_shrinkMax(DEFAULT_RECV_BUFFER_SIZE),
      m_shrinkCheckCnt(DEFAULT_SHRINK_COUNT)
{
    std::map<std::string, std::string>::iterator it = config.find("tcp.transport.recvBufferSize");
    if (it != config.end())
    {
        m_recvBufSize = atoi(it->second.c_str());
    }

    it = config.find("tcp.transport.shrinkCheckMax");
    if (it != config.end())
    {
        m_shrinkCheckCnt = atoi(it->second.c_str());
    }

    if (SocketInit() != 0)
    {
        m_state = CLIENT_STATE_UNINIT;
    }

    m_pRecvBuf = (char*)malloc(m_recvBufSize);
    m_state = (NULL == m_pRecvBuf) ? CLIENT_STATE_UNINIT : CLIENT_STATE_INITED;
    m_lastSendRecvTime = KPRUtil::GetCurrentTimeMillis();
}

TcpTransport::~TcpTransport()
{
    close();

    if (m_sfd != INVALID_SOCKET)
    {
        ::shutdown(m_sfd, SD_BOTH);
        ::closesocket(m_sfd);
        m_sfd = INVALID_SOCKET;
    }

    if (m_pRecvBuf)
    {
        free(m_pRecvBuf);
    }

    SocketUninit();
}


int TcpTransport::connect(const std::string& serverAddr, int timeoutMillis)
{
    long long endTime = KPRUtil::GetCurrentTimeMillis() + timeoutMillis;
    if (m_state == CLIENT_STATE_UNINIT)
    {
        return CLIENT_ERROR_INIT;
    }

    if (isConnected())
    {
        if (serverAddr.compare(m_serverAddr) == 0)
        {
            return CLIENT_ERROR_SUCCESS;
        }
        else
        {
            close();
        }
    }

    short port;
    std::string strAddr;

    if (!SplitURL(serverAddr, strAddr, port))
    {
        return CLIENT_ERROR_INVALID_URL;
    }

    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);

    sa.sin_addr.s_addr = inet_addr(strAddr.c_str());
    m_sfd = (int)socket(AF_INET, SOCK_STREAM, 0);

    if (MakeSocketNonblocking(m_sfd) == -1)
    {
    	::closesocket(m_sfd);
        return CLIENT_ERROR_CONNECT;
    }

    if (SetTcpNoDelay(m_sfd) == -1)
    {
        ::closesocket(m_sfd);
        return CLIENT_ERROR_CONNECT;
    }

    if (::connect(m_sfd, (struct sockaddr*)&sa, sizeof(sockaddr)) == -1)
    {
        int err = NET_ERROR;
        if (err == WSAEWOULDBLOCK || err == WSAEINPROGRESS)
        {
            kpr::Epoller epoller(false);
            epoller.create(1);
            epoller.add(m_sfd, 0, EPOLLOUT);
            int iRetCode = epoller.wait(endTime - KPRUtil::GetCurrentTimeMillis());
            if (iRetCode <= 0)
            {
                ::closesocket(m_sfd);
                return CLIENT_ERROR_CONNECT;
            }
            else if (iRetCode == 0)
            {
                ::closesocket(m_sfd);
                return CLIENT_ERROR_CONNECT;
            }

            const epoll_event& ev = epoller.get(0);
            if (ev.events & EPOLLERR || ev.events & EPOLLHUP)
            {
                ::closesocket(m_sfd);
                return CLIENT_ERROR_CONNECT;
            }

            int opterr = 0;
            socklen_t errlen = sizeof(opterr);
            if (getsockopt(m_sfd, SOL_SOCKET, SO_ERROR, &opterr, &errlen) == -1 || opterr)
            {
                ::closesocket(m_sfd);
                return CLIENT_ERROR_CONNECT;
            }
        }
        else
        {
            ::closesocket(m_sfd);
            return CLIENT_ERROR_CONNECT;
        }
    }

    m_serverAddr = serverAddr;
    m_state = CLIENT_STATE_CONNECTED;
    m_recvBufUsed = 0;
    m_lastSendRecvTime = KPRUtil::GetCurrentTimeMillis();

    return CLIENT_ERROR_SUCCESS;
}


bool TcpTransport::isConnected()
{
    return m_state == CLIENT_STATE_CONNECTED;
}

void TcpTransport::close()
{
    if (m_state == CLIENT_STATE_CONNECTED)
    {
        m_state = CLIENT_STATE_DISCONNECT;
    }
}

int TcpTransport::sendData(const char* pBuffer, int len, int timeOut)
{
    kpr::ScopedLock<kpr::Mutex> lock(m_sendLock);
    return sendOneMsg(pBuffer, len, timeOut > 0 ? timeOut : 0);
}

int TcpTransport::sendOneMsg(const char* pBuffer, int len, int nTimeOut)
{
    int pos = 0;
    long long endTime = KPRUtil::GetCurrentTimeMillis() + nTimeOut;

    while (len > 0 && m_state == CLIENT_STATE_CONNECTED)
    {
        int ret = send(m_sfd, pBuffer + pos, len, 0);
        if (ret > 0)
        {
            len -= ret;
            pos += ret;
        }
        else if (ret == 0)
        {
            close();
            break;
        }
        else
        {
            int err = NET_ERROR;
            if (err == WSAEWOULDBLOCK || err == EAGAIN)
            {
                kpr::Epoller epoller(false);
                epoller.create(1);
                epoller.add(m_sfd, 0, EPOLLOUT);
                int iRetCode = epoller.wait(endTime - KPRUtil::GetCurrentTimeMillis());
                if (iRetCode <= 0)
                {
                    close();
                    break;
                }
                else if (iRetCode == 0)
                {
                    close();
                    break;
                }

                const epoll_event& ev = epoller.get(0);
                if (ev.events & EPOLLERR || ev.events & EPOLLHUP)
                {
                    close();
                    break;
                }
            }
            else
            {
                close();
                break;
            }
        }
    }
    m_lastSendRecvTime = KPRUtil::GetCurrentTimeMillis();

    return (len == 0) ? 0 : -1;
}


int TcpTransport::recvMsg()
{
    int ret = recv(m_sfd, m_pRecvBuf + m_recvBufUsed, m_recvBufSize - m_recvBufUsed, 0);

    if (ret > 0)
    {
        m_recvBufUsed += ret;
    }
    else if (ret == 0)
    {
        close();
        ret = -1;
    }
    else if (ret < 0)
    {
        int err = NET_ERROR;
        if (err == WSAEWOULDBLOCK || err == EAGAIN || err == EINTR)
        {
            ret = 0;
        }
        else
        {
            close();
        }
    }
    m_lastSendRecvTime = KPRUtil::GetCurrentTimeMillis();

    return ret;
}

bool TcpTransport::resizeBuf(int nNewSize)
{
    char* newbuf = (char*)realloc(m_pRecvBuf, nNewSize);
    if (!newbuf)
    {
        return false;
    }

    m_pRecvBuf = newbuf;
    m_recvBufSize = nNewSize;

    return true;
}

void TcpTransport::tryShrink(int MsgLen)
{
    m_shrinkMax = MsgLen > m_shrinkMax ? MsgLen : m_shrinkMax;
    if (m_shrinkCheckCnt == 0)
    {
        m_shrinkCheckCnt = DEFAULT_SHRINK_COUNT;
        if (m_recvBufSize > m_shrinkMax)
        {
            resizeBuf(m_shrinkMax);
        }
    }
    else
    {
        m_shrinkCheckCnt--;
    }
}

int TcpTransport::getMsgSize(const char* pBuf)
{
    int len = 0;
    memcpy(&len, pBuf, sizeof(int));

    return ntohl(len) + 4;
}

int TcpTransport::recvData(std::list<std::string*>& dataList)
{
    int ret = recvMsg();
    processData(dataList);
    return ret;
}

void TcpTransport::processData(std::list<std::string*>& dataList)
{
    while (m_recvBufUsed > int(sizeof(int)))
    {
        int msgLen = 0;
        msgLen = getMsgSize(m_pRecvBuf);
        if (msgLen > m_recvBufSize)
        {
            if (resizeBuf(msgLen))
            {
                m_shrinkCheckCnt = DEFAULT_SHRINK_COUNT;
            }
            break;
        }
        else
        {
            tryShrink(msgLen);
        }

        if (m_recvBufUsed >= msgLen)
        {
            std::string* data = new std::string;
            data->assign(m_pRecvBuf, msgLen);
            dataList.push_back(data);
            m_recvBufUsed -= msgLen;

            memmove(m_pRecvBuf, m_pRecvBuf + msgLen, m_recvBufUsed);
        }
        else
        {
            break;
        }
    }
}

SOCKET TcpTransport::getSocket()
{
    return m_sfd;
}

std::string& TcpTransport::getServerAddr()
{
    return m_serverAddr;
}

unsigned long long TcpTransport::getLastSendRecvTime()
{
	return m_lastSendRecvTime;
}


}
