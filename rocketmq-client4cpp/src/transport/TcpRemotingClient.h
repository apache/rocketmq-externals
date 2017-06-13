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

#ifndef __TCPREMOTINGCLIENT_H__
#define __TCPREMOTINGCLIENT_H__

#include <map>
#include <string>
#include <list>

#include "RocketMQClient.h"
#include "SocketUtil.h"
#include "Epoller.h"
#include "RemotingCommand.h"
#include "Thread.h"
#include "ThreadPool.h"
#include "ThreadPoolWork.h"
#include "RemoteClientConfig.h"
#include "TcpTransport.h"
#include "ScopedLock.h"
#include "KPRUtil.h"
#include "Semaphore.h"
#include "ResponseFuture.h"

namespace rmq
{
    class TcpTransport;
    class InvokeCallback;
    class TcpRemotingClient;
    class ResponseFuture;
    class TcpRequestProcessor;

    class ProcessDataWork : public kpr::ThreadPoolWork
    {
    public:
        ProcessDataWork(TcpRemotingClient* pClient, TcpTransport* pTts, std::string* pData);
        virtual ~ProcessDataWork();
        virtual void Do();

    private:
        TcpRemotingClient* m_pClient;
		TcpTransport* m_pTts;
        std::string* m_pData;
    };
	typedef kpr::RefHandleT<ProcessDataWork> ProcessDataWorkPtr;

    class TcpRemotingClient
    {
		class EventThread : public kpr::Thread
        {
        public:
            EventThread(TcpRemotingClient& client)
                : Thread("NetThread"), m_client(client)
            {
            }

            void Run()
            {
                m_client.run();
            }

        private :
            TcpRemotingClient& m_client;
        };
        friend class EventThread;
        friend class ProcessDataWork;

	public:
		static const int s_LockTimeoutMillis = 3000;
		static const int s_CheckIntervalMillis = 1000;
		static const int s_ClientOnewaySemaphoreValue = 2048;
		static const int s_ClientAsyncSemaphoreValue = 2048;

    public:
        TcpRemotingClient(const RemoteClientConfig& config);
        virtual ~TcpRemotingClient();
        virtual void start();
        virtual void shutdown();

        void updateNameServerAddressList(const std::vector<std::string>& addrs);
        std::vector<std::string> getNameServerAddressList();
		void registerProcessor(int requestCode, TcpRequestProcessor* pProcessor);

        RemotingCommand* invokeSync(const std::string& addr, RemotingCommand* pRequest, int timeoutMillis) ;
        void invokeAsync(const std::string& addr, RemotingCommand* pRequest, int timeoutMillis, InvokeCallback* invokeCallback);
        int invokeOneway(const std::string& addr, RemotingCommand* pRequest, int timeoutMillis);

    private:
		void run();
        int  sendCmd(TcpTransport* pTts, RemotingCommand* pRequest, int timeoutMillis);
        void removeTTS(TcpTransport* pTts, bool isDisConnected = false);
        void processData(TcpTransport* pTts, std::string* data);
        void handleTimerEvent();
		void scanResponseTable();
		void scanCloseTransportTable();

        void processMessageReceived(TcpTransport* pTts, RemotingCommand* pCmd);
        void processRequestCommand(TcpTransport* pTts, RemotingCommand* pCmd);
        void processResponseCommand(TcpTransport* pTts, RemotingCommand* pCmd);

        TcpTransport* getAndCreateTransport(const std::string& addr, int timeoutMillis);
		TcpTransport* getAndCreateNameserverTransport(int timeoutMillis);
		TcpTransport* createTransport(const std::string& addr, int timeoutMillis);

        RemotingCommand* invokeSyncImpl(TcpTransport* pTts, RemotingCommand* pRequest, int timeoutMillis) ;
        void invokeAsyncImpl(TcpTransport* pTts, RemotingCommand* pRequest, int timeoutMillis, InvokeCallback* pInvokeCallback);
        int invokeOnewayImpl(TcpTransport* pTts, RemotingCommand* pRequest, int timeoutMillis);

    private:
        bool m_stop;
        kpr::Epoller m_epoller;
        RemoteClientConfig m_config;

		kpr::Semaphore m_semaphoreOneway;
		kpr::Semaphore m_semaphoreAsync;

        std::map<std::string , TcpTransport*> m_transportTable;
        kpr::RWMutex m_transportTableLock;

		std::list<TcpTransport*> m_closeTransportTable;
		kpr::Mutex m_closeTransportTableLock;

        std::map<int, ResponseFuturePtr> m_responseTable;
        kpr::RWMutex m_responseTableLock;

        std::vector<std::string> m_namesrvAddrList;
		kpr::AtomicInteger m_namesrvIndex;
		kpr::AtomicReference<std::string> m_namesrvAddrChoosed;
		kpr::Mutex m_namesrvAddrChoosedLock;

        kpr::ThreadPoolPtr m_pNetThreadPool;
		kpr::ThreadPtr m_pEventThread;

        TcpRequestProcessor* m_pDefaultRequestProcessor;
        std::map<int, TcpRequestProcessor*> m_processorTable;
    };
}

#endif
