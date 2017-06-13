/**
* Copyright (C) 2013 suwenkuang ,hooligan_520@qq.com
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

#ifndef __KPR_EPOLLER_H_
#define __KPR_EPOLLER_H_

#include <unistd.h>
#include <sys/epoll.h>
#include <cassert>

namespace kpr
{


class Epoller
{
public:
    Epoller(bool bEt = true);
    ~Epoller();


    void create(int max_connections);

    void add(int fd, long long data, __uint32_t event);
    void mod(int fd, long long data, __uint32_t event);
    void del(int fd, long long data, __uint32_t event);

    int wait(int millsecond);

    struct epoll_event& get(int i)
    {
        assert(_pevs != 0);
        return _pevs[i];
    }

protected:
    void ctrl(int fd, long long data, __uint32_t events, int op);

protected:
    int _iEpollfd;
    int _max_connections;
    struct epoll_event* _pevs;
    bool _et;
};

}
#endif


