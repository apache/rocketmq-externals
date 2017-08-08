/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SOCKETUTIL_H__
#define __SOCKETUTIL_H__

#ifdef WIN32
#include <WS2tcpip.h>
#include <Windows.h>
#include <Winsock2.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <net/if.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#endif
#include <sys/socket.h>
#include "UtilAll.h"

namespace metaq {
//<!************************************************************************
/**
* IP:PORT
*/
sockaddr IPPort2socketAddress(int host, int port);
string socketAddress2IPPort(sockaddr addr);
void socketAddress2IPPort(sockaddr addr, int& host, int& port);

string socketAddress2String(sockaddr addr);
string getHostName(sockaddr addr);

uint64 h2nll(uint64 v);
uint64 n2hll(uint64 v);

//<!************************************************************************
}  //<!end namespace;

#endif
