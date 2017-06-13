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
#include "SocketUtil.h"
#include "MixAll.h"
#include "MQClientException.h"


namespace rmq
{

int SocketInit()
{
    signal(SIGPIPE, SIG_IGN);

    return 0;
}

int MakeSocketNonblocking(SOCKET fd)
{
    int flags = fcntl(fd, F_GETFL, 0);
    assert(flags != -1);
    flags = (flags | O_NONBLOCK);
    return fcntl(fd, F_SETFL, flags);
}

int SetTcpNoDelay(SOCKET fd)
{
    int flag = 1;
    return setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (const char*)&flag, sizeof(flag));
}

bool SplitURL(const std::string& serverURL, std::string& addr, short& nPort)
{
    size_t pos = serverURL.find(':');
    if (pos == std::string::npos)
    {
        return false;
    }

    addr = serverURL.substr(0, pos);
    if (0 == addr.compare("localhost"))
    {
        addr = "127.0.0.1";
    }
    pos++;

    std::string port = serverURL.substr(pos, serverURL.length() - pos);
    nPort = atoi(port.c_str());
    return true;
}

sockaddr string2SocketAddress(const std::string& addrString)
{
    std::string strAddr;
    short port;
    SplitURL(addrString, strAddr, port);

    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);

    sa.sin_addr.s_addr = inet_addr(strAddr.c_str());

    sockaddr addr;
    memcpy(&addr, &sa, sizeof(sockaddr));

    return addr;
}

std::string socketAddress2String(sockaddr addr)
{
    sockaddr_in in;
    memcpy(&in, &addr, sizeof(sockaddr));

	std::stringstream ss;
    ss << inet_ntoa(in.sin_addr) << ":" << in.sin_port;

    return ss.str();
}

void GetLocalAddrs(std::vector<unsigned int>& addrs)
{
    addrs.clear();

    struct ifconf ifc;
    ifc.ifc_buf = NULL;
    ifc.ifc_len = 0;

    int sfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sfd != INVALID_SOCKET)
    {
        int ret = ioctl(sfd, SIOCGIFCONF, (char*)&ifc);

        if (ret != -1)
        {
            ifc.ifc_req = (struct ifreq*)malloc(ifc.ifc_len);
            ret = ioctl(sfd, SIOCGIFCONF, (char*)&ifc);
            if (ret != -1)
            {
                for (size_t i = 0; i < ifc.ifc_len / sizeof(struct ifreq); i++)
                {
                    struct sockaddr* sa = (struct sockaddr*) & (ifc.ifc_req[i].ifr_addr);
                    if (AF_INET == sa->sa_family)
                    {
                        unsigned int addr = ((struct sockaddr_in*)sa)->sin_addr.s_addr;
                        addrs.push_back(htonl(addr));
                    }
                }
            }

            free(ifc.ifc_req);
            ifc.ifc_req = NULL;
        }

        close(sfd);
    }

    if (addrs.empty())
    {
        char hostname[1024];

        int ret = gethostname(hostname, sizeof(hostname));
        if (ret == 0)
        {
            struct addrinfo* result = NULL;
            struct addrinfo* ptr = NULL;
            struct addrinfo hints;

            memset(&hints, 0, sizeof(hints));
            hints.ai_family = AF_INET;
            hints.ai_socktype = SOCK_STREAM;
            hints.ai_protocol = IPPROTO_TCP;

            ret = getaddrinfo(hostname, NULL, &hints, &result);
            if (ret == 0)
            {
                for (ptr = result; ptr != NULL ; ptr = ptr->ai_next)
                {

                    struct sockaddr_in*  sockaddr_ipv4 = (struct sockaddr_in*) ptr->ai_addr;
                    addrs.push_back(ntohl(sockaddr_ipv4->sin_addr.s_addr));
                }
            }

            freeaddrinfo(result);
        }
    }

    std::vector<unsigned int>::iterator it = addrs.begin();
    for (; it != addrs.end();)
    {
        if (*it >= 0x7F000000U && *it < 0x80000000U)
        {
            it = addrs.erase(it);
        }
        else
        {
            it++;
        }
    }

    if (addrs.empty())
    {
        addrs.push_back(INADDR_LOOPBACK);
    }
}

std::string getLocalAddress()
{
    std::vector<unsigned int> addrs;
    GetLocalAddrs(addrs);
    struct in_addr addr;
    addr.s_addr = htonl(addrs[0]);

    return inet_ntoa(addr);
}

std::string getHostName(sockaddr addr)
{
    sockaddr_in in;
    memcpy(&in, &addr, sizeof(sockaddr));

    struct hostent* remoteHost = gethostbyaddr((char*) & (in.sin_addr), 4, AF_INET);
    char** alias = remoteHost->h_aliases;
    if (*alias != 0)
    {
        return *alias;
    }
    else
    {
        return inet_ntoa(in.sin_addr);
    }
}


unsigned long long swapll(unsigned long long v)
{
#ifdef ENDIANMODE_BIG
    return v;
#else
    unsigned long long ret = ((v << 56)
                              | ((v & 0xff00) << 40)
                              | ((v & 0xff0000) << 24)
                              | ((v & 0xff000000) << 8)
                              | ((v >> 8) & 0xff000000)
                              | ((v >> 24) & 0xff0000)
                              | ((v >> 40) & 0xff00)
                              | (v >> 56));

    return ret;
#endif
}

unsigned long long h2nll(unsigned long long v)
{
    return swapll(v);
}

unsigned long long n2hll(unsigned long long v)
{
    return swapll(v);
}

std::string socketAddress2IPPort(sockaddr addr)
{
    sockaddr_in in;
    memcpy(&in, &addr, sizeof(sockaddr));

    char tmp[32];
    snprintf(tmp, sizeof(tmp), "%s:%d", inet_ntoa(in.sin_addr), ntohs(in.sin_port));

    std::string ipport = tmp;
    return ipport;
}


}
