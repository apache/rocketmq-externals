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
#include "ClientRemotingProcessor.h"
#include "MQProtos.h"
#include "TcpTransport.h"
#include "RemotingCommand.h"
#include "MQClientFactory.h"
#include "CommandCustomHeader.h"
#include "ConsumerRunningInfo.h"



namespace rmq
{

ClientRemotingProcessor::ClientRemotingProcessor(MQClientFactory* pMQClientFactory)
    : m_pMQClientFactory(pMQClientFactory)
{

}

RemotingCommand* ClientRemotingProcessor::processRequest(TcpTransport* pTts, RemotingCommand* pRequest)
{
    int code = pRequest->getCode();
    switch (code)
    {
        case CHECK_TRANSACTION_STATE_VALUE:
            return checkTransactionState(pTts, pRequest);
        case NOTIFY_CONSUMER_IDS_CHANGED_VALUE:
            return notifyConsumerIdsChanged(pTts, pRequest);
        case RESET_CONSUMER_CLIENT_OFFSET_VALUE:
            return resetOffset(pTts, pRequest);
        case GET_CONSUMER_STATUS_FROM_CLIENT_VALUE:
            return getConsumeStatus(pTts, pRequest);
        case GET_CONSUMER_RUNNING_INFO_VALUE:
            return getConsumerRunningInfo(pTts, pRequest);
        case CONSUME_MESSAGE_DIRECTLY_VALUE:
            return consumeMessageDirectly(pTts, pRequest);
        default:
            break;
    }

    return NULL;
}

RemotingCommand* ClientRemotingProcessor::checkTransactionState(TcpTransport* pTts, RemotingCommand* pRequest)
{
    //TODO
    return NULL;
}

RemotingCommand* ClientRemotingProcessor::notifyConsumerIdsChanged(TcpTransport* pTts, RemotingCommand* pRequest)
{
    try
    {
        NotifyConsumerIdsChangedRequestHeader* extHeader = (NotifyConsumerIdsChangedRequestHeader*)pRequest->getCommandCustomHeader();
        RMQ_INFO("receive broker's notification[{%s}], the consumer group: {%s} changed, rebalance immediately",
                pTts->getServerAddr().c_str(),
                extHeader->consumerGroup.c_str());
        m_pMQClientFactory->rebalanceImmediately();
    }
    catch (std::exception& e)
    {
        RMQ_ERROR("notifyConsumerIdsChanged exception: %s", e.what());
    }

    return NULL;
}

RemotingCommand* ClientRemotingProcessor::resetOffset(TcpTransport* pTts, RemotingCommand* pRequest)
{
    //TODO
    return NULL;
}


RemotingCommand* ClientRemotingProcessor::getConsumeStatus(TcpTransport* pTts, RemotingCommand* pRequest)
{
    //TODO
    return NULL;
}


RemotingCommand* ClientRemotingProcessor::getConsumerRunningInfo(TcpTransport* pTts, RemotingCommand* pRequest)
{
	return NULL;

	/*
    GetConsumerRunningInfoRequestHeader* requestHeader = (GetConsumerRunningInfoRequestHeader)pRequest->getCommandCustomHeader();
	RemotingCommand* pResponse = RemotingCommand::createResponseCommand(NULL);

	pResponse = RemotingCommand::createResponseCommand(
					REQUEST_CODE_NOT_SUPPORTED_VALUE, "request type not supported", NULL);
	pResponse->setOpaque(pCmd->getOpaque());

    ConsumerRunningInfo* consumerRunningInfo = m_pMQClientFactory->consumerRunningInfo(requestHeader->consumerGroup);
    if (NULL != consumerRunningInfo) {
        response.setCode(ResponseCode.SUCCESS);
        response.setBody(consumerRunningInfo.encode());
    } else {
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark(String.format("The Consumer Group <%s> not exist in this consumer",
                requestHeader.getConsumerGroup()));
    }
    return pResponse;

	// java
    final RemotingCommand response = RemotingCommand.createResponseCommand(null);
    final GetConsumerRunningInfoRequestHeader requestHeader =
            (GetConsumerRunningInfoRequestHeader) request
                    .decodeCommandCustomHeader(GetConsumerRunningInfoRequestHeader.class);

    ConsumerRunningInfo consumerRunningInfo =
            this.mqClientFactory.consumerRunningInfo(requestHeader.getConsumerGroup());
    if (null != consumerRunningInfo) {
        if (requestHeader.isJstackEnable()) {
            String jstack = UtilAll.jstack();
            consumerRunningInfo.setJstack(jstack);
        }

        response.setCode(ResponseCode.SUCCESS);
        response.setBody(consumerRunningInfo.encode());
    } else {
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark(String.format("The Consumer Group <%s> not exist in this consumer",
                requestHeader.getConsumerGroup()));
    }

    return response;
    */
}


RemotingCommand* ClientRemotingProcessor::consumeMessageDirectly(TcpTransport* pTts, RemotingCommand* pRequest)
{
    //TODO
    return NULL;
}


}
