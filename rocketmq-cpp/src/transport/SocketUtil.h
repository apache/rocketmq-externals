/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __SOCKETUTIL_H__
#define __SOCKETUTIL_H__

#ifdef WIN32
#include <WS2tcpip.h>
#include <Winsock2.h>
#include <Windows.h>
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
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#endif

#include "UtilAll.h"

namespace rocketmq {
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
