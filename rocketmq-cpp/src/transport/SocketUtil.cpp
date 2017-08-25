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
#include "SocketUtil.h"

namespace rocketmq {
//<!***************************************************************************
sockaddr IPPort2socketAddress(int host, int port) {
  struct sockaddr_in sa;
  sa.sin_family = AF_INET;
  sa.sin_port = htons((uint16)port);
  sa.sin_addr.s_addr = htonl(host);

  sockaddr bornAddr;
  memcpy(&bornAddr, &sa, sizeof(sockaddr));
  return bornAddr;
}

string socketAddress2IPPort(sockaddr addr) {
  sockaddr_in sa;
  memcpy(&sa, &addr, sizeof(sockaddr));

  char tmp[32];
  sprintf(tmp, "%s:%d", inet_ntoa(sa.sin_addr), ntohs(sa.sin_port));

  string ipport = tmp;
  return ipport;
}

void socketAddress2IPPort(sockaddr addr, int& host, int& port) {
  struct sockaddr_in sa;
  memcpy(&sa, &addr, sizeof(sockaddr));

  host = ntohl(sa.sin_addr.s_addr);
  port = ntohs(sa.sin_port);
}

string socketAddress2String(sockaddr addr) {
  sockaddr_in in;
  memcpy(&in, &addr, sizeof(sockaddr));

  return inet_ntoa(in.sin_addr);
}

string getHostName(sockaddr addr) {
  sockaddr_in in;
  memcpy(&in, &addr, sizeof(sockaddr));

  struct hostent* remoteHost = gethostbyaddr((char*)&(in.sin_addr), 4, AF_INET);
  char** alias = remoteHost->h_aliases;
  if (*alias != 0) {
    return *alias;
  } else {
    return inet_ntoa(in.sin_addr);
  }
}

uint64 swapll(uint64 v) {
#ifdef ENDIANMODE_BIG
  return v;
#else
  uint64 ret = ((v << 56) | ((v & 0xff00) << 40) | ((v & 0xff0000) << 24) |
                ((v & 0xff000000) << 8) | ((v >> 8) & 0xff000000) |
                ((v >> 24) & 0xff0000) | ((v >> 40) & 0xff00) | (v >> 56));

  return ret;
#endif
}

uint64 h2nll(uint64 v) { return swapll(v); }

uint64 n2hll(uint64 v) { return swapll(v); }
}  //<!end namespace;
