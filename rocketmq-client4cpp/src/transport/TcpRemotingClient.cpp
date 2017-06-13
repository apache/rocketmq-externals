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
#include "TcpRemotingClient.h"
#include "MQClientException.h"
#include "TcpRequestProcessor.h"
#include "MQProtos.h"
#include "ThreadPoolWork.h"

namespace rmq
{


ProcessDataWork::ProcessDataWork(TcpRemotingClient* pClient, TcpTransport* pTts, std::string* pData)
    : m_pClient(pClient), m_pTts(pTts), m_pData(pData)
{
}

ProcessDataWork::~ProcessDataWork()
{
	delete m_pData;
}

void ProcessDataWork::Do()
{
    try
    {
        m_pClient->processData(m_pTts, m_pData);
    }
    catch (std::exception& e)
    {
    	RMQ_ERROR("processDataWork catch Exception: %s", e.what());
    }
    catch (...)
    {
    	RMQ_ERROR("processDataWork catch Exception");
    }
}

TcpRemotingClient::TcpRemotingClient(const RemoteClientConfig& config)
    : m_stop(false), m_epoller(false), m_config(config),
      m_semaphoreOneway(s_ClientOnewaySemaphoreValue), m_semaphoreAsync(s_ClientAsyncSemaphoreValue)
{
    m_pNetThreadPool = new kpr::ThreadPool("NetClientThreadPool", 5, 5, 20);
    m_pEventThread = new EventThread(*this);
    SocketInit();
    m_epoller.create(10240);
}

TcpRemotingClient::~TcpRemotingClient()
{
    SocketUninit();
}

void TcpRemotingClient::start()
{
    RMQ_DEBUG("TcpRemotingClient::start()");
    m_pEventThread->Start();
}

void TcpRemotingClient::shutdown()
{
    RMQ_DEBUG("TcpRemotingClient::shutdown()");
    m_stop = true;
    m_pNetThreadPool->Destroy();
    m_pEventThread->Join();
}

/*
void printMsg(const std::string& prefix, const char* pData, int len)
{
	int headLen;
    memcpy(&headLen, pData + 4, 4);
    headLen = ntohl(headLen);

    RMQ_DEBUG("%s|decode[%d,%d,%d]|%s%s", prefix.c_str(), len, headLen, len - 8 - headLen, std::string(pData + 8, headLen).c_str(),
              std::string(pData + 8 + headLen, len - 8 - headLen).c_str());
}
*/

void TcpRemotingClient::run()
{
    RMQ_INFO("EventThread run begin: %lld", KPRUtil::GetCurrentTimeMillis());
    do
    {
        try
        {
            int nfds = m_epoller.wait(500);
            if (nfds > 0)
            {
                int ret = 0;
                std::vector<TcpTransport*> errTts;
                for (int i = 0; i < nfds && !m_stop; ++i)
                {
                    const epoll_event& ev = m_epoller.get(i);
                    std::map<std::string , TcpTransport*>::iterator it;
                    {
                        kpr::ScopedRLock<kpr::RWMutex> lock(m_transportTableLock);
                        it = m_transportTable.find((char*)ev.data.ptr);
                        if (it == m_transportTable.end())
                        {
                            continue;
                        }
                    }

                    TcpTransport* pTts = it->second;
                    if (ev.events & EPOLLERR || ev.events & EPOLLHUP)
                    {
                    	RMQ_ERROR("recvData fail, err=%d(%s), pts=%p", errno, strerror(errno), pTts);
                        errTts.push_back(pTts);
                    }

                    if (ev.events & EPOLLIN)
                    {
                    	std::list<std::string*> dataList;
                        ret = pTts->recvData(dataList);
                        if (ret < 0)
                        {
                        	RMQ_ERROR("recvData fail, ret=%d, errno=%d, pts=%p", ret, NET_ERROR, pTts);
                            errTts.push_back(pTts);
                        }

                        if (dataList.size() > 0)
                        {
                        	for (typeof(dataList.begin()) it = dataList.begin();
                        		it != dataList.end(); it++)
                        	{
                        		//printMsg("run", (*it)->c_str(), (*it)->size());
                        		kpr::ThreadPoolWorkPtr work = new ProcessDataWork(this, pTts, *it);
								m_pNetThreadPool->AddWork(work);
							}
                        }
                    }
                }

                std::vector<TcpTransport*>::iterator itErr = errTts.begin();
                for (; itErr != errTts.end(); itErr++)
                {
                    removeTTS(*itErr, true);
                }
            }

            handleTimerEvent();
        }
        catch (...)
        {
			RMQ_ERROR("TcpRemotingClient.run catch exception");
        }
    }
    while (!m_stop);
    handleTimerEvent();

    RMQ_INFO("EventThread run end: %lld", KPRUtil::GetCurrentTimeMillis());
}


void TcpRemotingClient::updateNameServerAddressList(const std::vector<std::string>& addrs)
{
    m_namesrvAddrList = addrs;
    m_namesrvIndex = 0;
}

std::vector<std::string> TcpRemotingClient::getNameServerAddressList()
{
    return m_namesrvAddrList;
}

void TcpRemotingClient::registerProcessor(int requestCode, TcpRequestProcessor* pProcessor)
{
    m_processorTable[requestCode] = pProcessor;
}


RemotingCommand* TcpRemotingClient::invokeSync(const std::string& addr,
        RemotingCommand* pRequest,
        int timeoutMillis)
{
    TcpTransport* pTts = getAndCreateTransport(addr, timeoutMillis);
    if (pTts != NULL && pTts->isConnected())
    {
    	RemotingCommand* pResponse = NULL;
    	try
    	{
        	pResponse = invokeSyncImpl(pTts, pRequest, timeoutMillis);
        }
        catch(const RemotingSendRequestException& e)
        {
			RMQ_WARN("invokeSync: send pRequest exception, so close the channel[{%s}]",
				pTts->getServerAddr().c_str());
            removeTTS(pTts, false);
            throw e;
        }
        catch(const RemotingTimeoutException& e)
        {
			RMQ_WARN("invokeSync: wait response timeout exception, the channel[{%s}], timeout=%d",
				pTts->getServerAddr().c_str(), timeoutMillis);
			throw e;
        }

        return pResponse;
    }
    else
    {
        removeTTS(pTts, false);
        THROW_MQEXCEPTION(RemotingConnectException, "connect fail", -1);
        //return NULL;
    }
}

void TcpRemotingClient::invokeAsync(const std::string& addr,
                                   RemotingCommand* pRequest,
                                   int timeoutMillis,
                                   InvokeCallback* pInvokeCallback)
{
    TcpTransport* pTts = getAndCreateTransport(addr, timeoutMillis);
    if (pTts != NULL && pTts->isConnected())
    {
    	try
    	{
        	this->invokeAsyncImpl(pTts, pRequest, timeoutMillis, pInvokeCallback);
        }
        catch (const RemotingSendRequestException& e)
        {
			RMQ_WARN("invokeAsync: send pRequest exception, so close the channel[{%s}]", addr.c_str());
            removeTTS(pTts, false);
            throw e;
        }

        return;
    }
    else
    {
        removeTTS(pTts, false);
        std::string msg;msg.append("connect to <").append(addr).append("> failed");
        THROW_MQEXCEPTION(RemotingConnectException, msg, -1);
    }
}

int TcpRemotingClient::invokeOneway(const std::string& addr,
                                    RemotingCommand* pRequest,
                                    int timeoutMillis)
{
    TcpTransport* pTts = getAndCreateTransport(addr, timeoutMillis);
    if (pTts != NULL && pTts->isConnected())
    {
        return invokeOnewayImpl(pTts, pRequest, timeoutMillis);
    }
    else
    {
        removeTTS(pTts, false);
        return -1;
    }
}


TcpTransport* TcpRemotingClient::getAndCreateTransport(const std::string& addr, int timeoutMillis)
{
	if (addr.empty())
	{
    	return getAndCreateNameserverTransport(timeoutMillis);
    }

	{
        kpr::ScopedRLock<kpr::RWMutex> lock(m_transportTableLock);
        std::map<std::string , TcpTransport*>::iterator it = m_transportTable.find(addr);
        if (it != m_transportTable.end())
        {
            return it->second;
        }
    }

    return this->createTransport(addr, timeoutMillis);
}


TcpTransport* TcpRemotingClient::createTransport(const std::string& addr, int timeoutMillis)
{
	TcpTransport* pTts = NULL;
	{
		kpr::ScopedRLock<kpr::RWMutex> lock(m_transportTableLock);
		std::map<std::string , TcpTransport*>::iterator it = m_transportTable.find(addr);
        if (it != m_transportTable.end())
        {
            return it->second;
        }
    }

	if (m_transportTableLock.TryWriteLock(s_LockTimeoutMillis))
	{
		std::map<std::string , TcpTransport*>::iterator it = m_transportTable.find(addr);
        if (it != m_transportTable.end())
        {
            return it->second;
        }

	    std::map<std::string , std::string> config;
	    pTts = new TcpTransport(config);
	    if (pTts->connect(addr, timeoutMillis) != CLIENT_ERROR_SUCCESS)
	    {
	        delete pTts;
	        pTts = NULL;

	        RMQ_INFO("[NETWORK]: CONNECT {%s} failed", addr.c_str());
	    }
	    else
	    {
		    m_transportTable[addr] = pTts;
	        m_epoller.add(pTts->getSocket(), (long long)((pTts->getServerAddr()).c_str()), EPOLLIN);

	        RMQ_INFO("[NETWORK]: CONNECT => {%s} success", addr.c_str());
        }
        m_transportTableLock.Unlock();
    }
    else
    {
    	RMQ_WARN("createTransport: try to lock m_transportTable, but timeout, {%d}ms", timeoutMillis);
    }

    return pTts;
}


TcpTransport* TcpRemotingClient::getAndCreateNameserverTransport(int timeoutMillis)
{
	TcpTransport* pTts = NULL;

	if (m_namesrvAddrChoosed.get() != NULL)
	{
		std::string addr = *m_namesrvAddrChoosed;
		if (!addr.empty())
		{
			pTts = getAndCreateTransport(addr, timeoutMillis);
	        if (pTts != NULL)
	        {
	        	return pTts;
	        }
		}
	}

	if (m_namesrvAddrChoosedLock.TryLock(s_LockTimeoutMillis))
	{
    	if (m_namesrvAddrChoosed.get() != NULL)
    	{
	    	std::string addr = *m_namesrvAddrChoosed;
	    	if (!addr.empty())
			{
				pTts = getAndCreateTransport(addr, timeoutMillis);
		        if (pTts != NULL)
		        {
		        	m_namesrvAddrChoosedLock.Unlock();
		        	return pTts;
		        }
			}
		}

		if (!m_namesrvAddrList.empty())
    	{
	        for (size_t i = 0; i < m_namesrvAddrList.size(); i++)
	        {
	            int index = abs(++m_namesrvIndex) % m_namesrvAddrList.size();
	            std::string& newAddr = m_namesrvAddrList.at(index);
	            m_namesrvAddrChoosed.set(&newAddr);
	            TcpTransport* pTts = getAndCreateTransport(newAddr, timeoutMillis);
	            if (pTts != NULL)
	            {
	            	m_namesrvAddrChoosedLock.Unlock();
	            	return pTts;
	            }
	        }
        }

        m_namesrvAddrChoosedLock.Unlock();
	}

	return NULL;
}


void TcpRemotingClient::handleTimerEvent()
{
	// every 1000ms
	static unsigned long long lastTime = 0;
	if (!m_stop && (int)(KPRUtil::GetCurrentTimeMillis() - lastTime) < s_CheckIntervalMillis)
	{
		return;
	}

    try
    {
    	lastTime = KPRUtil::GetCurrentTimeMillis();

    	this->scanResponseTable();

    	this->scanCloseTransportTable();
    }
    catch(...)
    {
    	RMQ_ERROR("scanResponseTable exception");
    }
}


void TcpRemotingClient::scanCloseTransportTable()
{
	if (m_closeTransportTable.empty())
	{
		return;
	}

	if (m_closeTransportTableLock.TryLock())
	{
		std::list<TcpTransport*>::iterator it;
		for( it = m_closeTransportTable.begin(); it != m_closeTransportTable.end(); )
		{
			TcpTransport* pTts = *it;
			long long diffTime = KPRUtil::GetCurrentTimeMillis() - pTts->getLastSendRecvTime();
			if (m_stop || (diffTime > 5000))
			{
				RMQ_WARN("remove close connection, %lld, {%s}", diffTime, pTts->getServerAddr().c_str());
				it = m_closeTransportTable.erase(it);
				delete pTts;
			}
			else
			{
				it++;
			}
		}
		m_closeTransportTableLock.Unlock();
	}
	else
	{
		RMQ_WARN("m_closeTransportTableLock TryLock fail");
	}
}


void TcpRemotingClient::scanResponseTable()
{
	kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
	for(typeof(m_responseTable.begin()) it = m_responseTable.begin();it != m_responseTable.end();)
	{
		long long diffTime = KPRUtil::GetCurrentTimeMillis() - it->second->getBeginTimestamp();
		if (m_stop || (diffTime > it->second->getTimeoutMillis() + 2000))
		{
			RMQ_WARN("remove timeout request, %lld, %s", diffTime, it->second->toString().c_str());
			try
			{
				it->second->executeInvokeCallback();
			}
			catch(...)
			{
				RMQ_WARN("scanResponseTable, operationComplete Exception");
			}
			it->second->release();
			m_responseTable.erase(it++);
		}
		else
		{
			it++;
		}
	}
}

void TcpRemotingClient::processData(TcpTransport* pTts, std::string* pData)
{
	//printMsg("processData", pData->c_str(), pData->size());
    RemotingCommand* pCmd = RemotingCommand::decode(pData->data(), (int)pData->size());
	if (pCmd == NULL)
	{
		RMQ_ERROR("invalid data format, len:%d, data: %s", (int)pData->size(), pData->c_str());
		return;
	}

    int code = 0;
    if (pCmd->isResponseType())
    {
        kpr::ScopedRLock<kpr::RWMutex> lock(m_responseTableLock);
        std::map<int, ResponseFuturePtr>::iterator it = m_responseTable.find(pCmd->getOpaque());
        if (it != m_responseTable.end())
        {
            code = it->second->getRequestCode();
        }
        else
        {
            RMQ_WARN("receive response, but not matched any request, maybe timeout or oneway, pCmd: %s", pCmd->toString().c_str());
            delete pCmd;
            return;
        }
    }
    else
    {
        code = pCmd->getCode();
    }

    pCmd->makeCustomHeader(code, pData->data(), (int)pData->size());
    if (pCmd->isResponseType())
    {
	    RMQ_DEBUG("[NETWORK]: RECV => {%s}, {opaque=%d, requst.code=%s(%d), response.code=%s(%d)}, %s",
	    	pTts->getServerAddr().c_str(), pCmd->getOpaque(), getMQRequestCodeString(code), code,
	    	getMQResponseCodeString(pCmd->getCode()), pCmd->getCode(), pCmd->toString().c_str());
    }
    else
    {
    	RMQ_DEBUG("[NETWORK]: RECV => {%s}, {opaque=%d, requst.code=%s(%d)}, %s",
	    	pTts->getServerAddr().c_str(), pCmd->getOpaque(),
	    	getMQRequestCodeString(code), code,	pCmd->toString().c_str());
    }

    processMessageReceived(pTts, pCmd);
}

RemotingCommand* TcpRemotingClient::invokeSyncImpl(TcpTransport* pTts,
        RemotingCommand* pRequest,
        int timeoutMillis)
{
    ResponseFuturePtr pResponseFuture = new ResponseFuture(
    	pRequest->getCode(), pRequest->getOpaque(), timeoutMillis,
        NULL, true, NULL);
    {
        kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
        m_responseTable.insert(std::pair<int, ResponseFuturePtr>(pRequest->getOpaque(), pResponseFuture));
    }

    int ret = sendCmd(pTts, pRequest, timeoutMillis);
    if (ret == 0)
    {
        pResponseFuture->setSendRequestOK(true);
    }
    else
    {
    	pResponseFuture->setSendRequestOK(false);
    	pResponseFuture->putResponse(NULL);
    	RMQ_WARN("send a pRequest command to channel <%s> failed.", pTts->getServerAddr().c_str());
    }

    RemotingCommand* pResponse = pResponseFuture->waitResponse(timeoutMillis);
    {
        kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
        std::map<int, ResponseFuturePtr>::iterator it = m_responseTable.find(pRequest->getOpaque());
        if (it != m_responseTable.end())
        {
            m_responseTable.erase(it);
        }
    }

    if (pResponse == NULL)
    {
        if (ret == 0)
        {
            std::stringstream oss;
            oss << "wait response on the channel <" << pTts->getServerAddr() << "> timeout," << timeoutMillis << "ms";
            THROW_MQEXCEPTION(RemotingTimeoutException, oss.str(), -1);
        }
        else
        {
            std::stringstream oss;
            oss << "send request to <" << pTts->getServerAddr() << "> failed";
            THROW_MQEXCEPTION(RemotingSendRequestException, oss.str(), -1);
        }
    }

    return pResponse;
}

void TcpRemotingClient::invokeAsyncImpl(TcpTransport* pTts,
                                       RemotingCommand* pRequest,
                                       int timeoutMillis,
                                       InvokeCallback* pInvokeCallback)
{
	bool acquired = m_semaphoreAsync.Wait(timeoutMillis);
	if (acquired)
	{
	    ResponseFuturePtr pResponseFuture = new ResponseFuture(
	    	pRequest->getCode(), pRequest->getOpaque(), timeoutMillis,
        	pInvokeCallback, false, &m_semaphoreAsync);
	    {
	        kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
	        m_responseTable.insert(std::pair<int, ResponseFuturePtr>(pRequest->getOpaque(), pResponseFuture));
	    }

	    int ret = sendCmd(pTts, pRequest, timeoutMillis);
	    if (ret == 0)
	    {
	        pResponseFuture->setSendRequestOK(true);
	    }
	    else
	    {
	    	pResponseFuture->setSendRequestOK(false);
	    	pResponseFuture->putResponse(NULL);
			{
		        kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
		        std::map<int, ResponseFuturePtr>::iterator it = m_responseTable.find(pRequest->getOpaque());
		        if (it != m_responseTable.end())
		        {
		            m_responseTable.erase(it);
		        }
	        }

	    	try
	    	{
	    		pResponseFuture->executeInvokeCallback();
	    	}
	    	catch (...)
	    	{
	            RMQ_WARN("executeInvokeCallback exception");
	        }
	        pResponseFuture->release();

	    	RMQ_WARN("send a pRequest command to channel <%s> failed, requet: %s",
	    		pTts->getServerAddr().c_str(), pRequest->toString().c_str());
	    }
    }
    else
    {
    	if (timeoutMillis <= 0)
    	{
            THROW_MQEXCEPTION(RemotingTooMuchRequestException, "invokeAsyncImpl invoke too fast", -1);
        }
        else
        {
        	std::string info = RocketMQUtil::str2fmt(
        		"invokeAsyncImpl wait semaphore timeout, %dms, semaphoreAsyncValue: %d, request: %s",
                timeoutMillis,
                m_semaphoreAsync.GetValue(),
                pRequest->toString().c_str()
            );
            RMQ_WARN("%s", info.c_str());
            THROW_MQEXCEPTION(RemotingTimeoutException, info, -1);
        }
    }

    return;
}

int TcpRemotingClient::invokeOnewayImpl(TcpTransport* pTts,
                                        RemotingCommand* pRequest,
                                        int timeoutMillis)
{
	pRequest->markOnewayRPC();

	bool acquired = m_semaphoreOneway.Wait(timeoutMillis);
	if (acquired)
	{
		int ret = sendCmd(pTts, pRequest, timeoutMillis);
		m_semaphoreOneway.Release();
	    if (ret != 0)
	    {
	    	RMQ_WARN("send a pRequest command to channel <%s> failed, requet: %s",
	    		pTts->getServerAddr().c_str(), pRequest->toString().c_str());
	    	THROW_MQEXCEPTION(RemotingSendRequestException, std::string("send request to <") + pTts->getServerAddr() + "> fail", -1);
	    }
    }
	else
	{
		if (timeoutMillis <= 0)
		{
			THROW_MQEXCEPTION(RemotingTooMuchRequestException, "invokeOnewayImpl invoke too fast", -1);
		}
		else
		{
			std::string info = RocketMQUtil::str2fmt(
				"invokeOnewayImpl wait semaphore timeout, %dms, semaphoreAsyncValue: %d, request: %s",
				timeoutMillis,
				m_semaphoreAsync.GetValue(),
				pRequest->toString().c_str()
			);
			RMQ_WARN("%s", info.c_str());
			THROW_MQEXCEPTION(RemotingTimeoutException, info, -1);
		}
	}

    return 0;
}

void TcpRemotingClient::processMessageReceived(TcpTransport* pTts, RemotingCommand* pCmd)
{
    try
    {
        switch (pCmd->getType())
        {
            case REQUEST_COMMAND:
                processRequestCommand(pTts, pCmd);
                break;
            case RESPONSE_COMMAND:
                processResponseCommand(pTts, pCmd);
                break;
            default:
                break;
        }
    }
    catch (std::exception& e)
    {
    	RMQ_ERROR("processMessageReceived catch Exception: %s", e.what());
    }
    catch (...)
    {
    	RMQ_ERROR("processMessageReceived catch Exception");
    }
}

void TcpRemotingClient::processRequestCommand(TcpTransport* pTts, RemotingCommand* pCmd)
{
	RMQ_DEBUG("receive request from server, cmd: %s", pCmd->toString().c_str());
	RemotingCommandPtr pResponse = NULL;
	std::map<int, TcpRequestProcessor*>::iterator it = m_processorTable.find(pCmd->getCode());
    if (it != m_processorTable.end())
    {
    	try
    	{
	        pResponse = it->second->processRequest(pTts, pCmd);
	        if (!pCmd->isOnewayRPC())
	        {
	        	if (pResponse.ptr() != NULL)
	        	{
	        		pResponse->setOpaque(pCmd->getOpaque());
	                pResponse->markResponseType();
	                int ret = this->sendCmd(pTts, pResponse, 3000);
	                if (ret != 0)
	                {
	                	RMQ_ERROR("process request over, but response failed");
	                }
	        	}
	        	else
	        	{
                    // ignore
	        	}
	        }
		}
		catch (const std::exception& e)
		{
			RMQ_ERROR("process request exception:%s", e.what());
			if (!pCmd->isOnewayRPC())
			{
				pResponse = RemotingCommand::createResponseCommand(
					SYSTEM_ERROR_VALUE, e.what(), NULL);
				pResponse->setOpaque(pCmd->getOpaque());
				int ret = this->sendCmd(pTts, pResponse, 3000);
				if (ret != 0)
                {
                	RMQ_ERROR("process request over, but response failed");
                }
			}
		}
    }
    else
    {
    	pResponse = RemotingCommand::createResponseCommand(
					REQUEST_CODE_NOT_SUPPORTED_VALUE, "request type not supported", NULL);
		pResponse->setOpaque(pCmd->getOpaque());
		int ret = this->sendCmd(pTts, pResponse, 3000);
		if (ret != 0)
        {
        	RMQ_ERROR("process request over, but pResponse failed");
        }
    }
	delete pCmd;
}

void TcpRemotingClient::processResponseCommand(TcpTransport* pTts, RemotingCommand* pCmd)
{
    ResponseFuturePtr res = NULL;
    {
        kpr::ScopedWLock<kpr::RWMutex> lock(m_responseTableLock);
        std::map<int, ResponseFuturePtr>::iterator it = m_responseTable.find(pCmd->getOpaque());
        if (it != m_responseTable.end())
        {
            res = it->second;
            res->release();
            m_responseTable.erase(it);
        }
    }

    if (res)
    {
        res->putResponse(pCmd);
        res->executeInvokeCallback();
    }
    else
    {
        RMQ_WARN("receive response, but not matched any request, cmd: %s", pCmd->toString().c_str());
        delete pCmd;
    }
}

int TcpRemotingClient::sendCmd(TcpTransport* pTts, RemotingCommand* pRequest, int timeoutMillis)
{
    pRequest->encode();
    int ret = pTts->sendData(pRequest->getData(), pRequest->getDataLen(), timeoutMillis);

    RMQ_DEBUG("[NETWORK]: SEND => {%s}, {opaque=%d, request.code=%s(%d), ret=%d, timeout=%d}, %s",
    	pTts->getServerAddr().c_str(), pRequest->getOpaque(),
    	getMQRequestCodeString(pRequest->getCode()), pRequest->getCode(),
    	ret, timeoutMillis, pRequest->toString().c_str());

    return ret;
}

void TcpRemotingClient::removeTTS(TcpTransport* pTts, bool isDisConnected)
{
    if (pTts)
    {
    	RMQ_INFO("[NETWORK]: %s  => {%s}", isDisConnected ? "DISCONNECT" : "CLOSE",
    		pTts->getServerAddr().c_str());

		bool bNeedClear = false;
        m_epoller.del(pTts->getSocket(), (long long)(pTts->getServerAddr().c_str()), 0);
        {
            kpr::ScopedWLock<kpr::RWMutex> lock(m_transportTableLock);
            std::map<std::string , TcpTransport*>::iterator it = m_transportTable.find(pTts->getServerAddr());
            if (it != m_transportTable.end())
            {
            	if (it->second == pTts)
            	{
            		m_transportTable.erase(it);
            		bNeedClear = true;
            	}
            }
        }

        if (bNeedClear)
        {
        	kpr::ScopedLock<kpr::Mutex> lock(m_closeTransportTableLock);
        	m_closeTransportTable.push_back(pTts);
        }
    }
}

}
