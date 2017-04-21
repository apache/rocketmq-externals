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
#include "CommandCustomHeader.h"

#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sstream>
#include <string>
#include <cstdlib>
#include "RemotingCommand.h"
#include "MQProtos.h"
#include "KPRUtil.h"
#include "UtilAll.h"

#include "json/json.h"

namespace rmq
{


CommandCustomHeader* CommandCustomHeader::decode(int code, Json::Value& data, bool isResponseType)
{
	CommandCustomHeader* pCustomHeader = NULL;

	try
	{
	    if (isResponseType)
	    {
	        switch (code)
	        {
	            case SEND_MESSAGE_VALUE:
	            case SEND_MESSAGE_V2_VALUE:
	                pCustomHeader = SendMessageResponseHeader::decode(data);
	                break;
	            case PULL_MESSAGE_VALUE:
	                pCustomHeader = PullMessageResponseHeader::decode(data);
	                break;
	            case QUERY_CONSUMER_OFFSET_VALUE:
	                pCustomHeader = QueryConsumerOffsetResponseHeader::decode(data);
	                break;
	            case SEARCH_OFFSET_BY_TIMESTAMP_VALUE:
	            	pCustomHeader = SearchOffsetResponseHeader::decode(data);
	            	break;
	           	case GET_MAX_OFFSET_VALUE:
	            	pCustomHeader = GetMaxOffsetResponseHeader::decode(data);
	            	break;
	            case GET_MIN_OFFSET_VALUE:
	            	pCustomHeader = GetMinOffsetResponseHeader::decode(data);
	            	break;
	            case GET_EARLIEST_MSG_STORETIME_VALUE:
	            	pCustomHeader = GetEarliestMsgStoretimeResponseHeader::decode(data);
	            	break;
	            case QUERY_MESSAGE_VALUE:
	            	pCustomHeader = QueryMessageResponseHeader::decode(data);
	            	break;
	            case GET_KV_CONFIG_VALUE:
	            	pCustomHeader = GetKVConfigResponseHeader::decode(data);
	            	break;

	            default:
	                break;
	        }
	    }
	    else
	    {
			switch (code)
	        {
	            case NOTIFY_CONSUMER_IDS_CHANGED_VALUE:
	                pCustomHeader = NotifyConsumerIdsChangedRequestHeader::decode(data);
	                break;
	            case GET_CONSUMER_RUNNING_INFO_VALUE:
	            	pCustomHeader = GetConsumerRunningInfoRequestHeader::decode(data);
	            	break;
	            default:
	                break;
	        }
	    }
    }
    catch(std::exception& e)
    {
    	if (pCustomHeader != NULL)
    	{
    		delete pCustomHeader;
    		pCustomHeader = NULL;
    	}
    	RMQ_ERROR("CommandCustomHeader decode exception, %d, %d, %s, %s",
    		code, isResponseType, UtilAll::toString(data).c_str(), e.what());
    }
    catch(...)
    {
    	if (pCustomHeader != NULL)
    	{
    		delete pCustomHeader;
    		pCustomHeader = NULL;
    	}
    	RMQ_ERROR("CommandCustomHeader decode exception, %d, %d, %s",
    		code, isResponseType, UtilAll::toString(data).c_str());
    }

    return pCustomHeader;
}


////////////////////////////////////////////////////////////////////////////////
//GET_ROUTEINTO_BY_TOPIC_VALUE
////////////////////////////////////////////////////////////////////////////////
void GetRouteInfoRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss << "{"
       << "\"topic\":\"" << topic << "\""
       << "}";

    outData = ss.str();
}


////////////////////////////////////////////////////////////////////////////////
// UPDATE_AND_CREATE_TOPIC_VALUE
////////////////////////////////////////////////////////////////////////////////
void CreateTopicRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"topic\":\"" << topic << "\","
       << "\"defaultTopic\":\"" << defaultTopic << "\","
	   << "\"readQueueNums\":\"" << readQueueNums << "\","
	   << "\"writeQueueNums\":\"" << writeQueueNums << "\","
	   << "\"perm\":\"" << perm << "\","
	   << "\"topicFilterType\":\"" << topicFilterType << "\","
	   << "\"topicSysFlag\":\"" << topicFilterType << "\","
	   << "\"order\":\"" << topicFilterType << "\""
       << "}";

    outData = ss.str();
}


////////////////////////////////////////////////////////////////////////////////
// SEND_MESSAGE_VALUE/SEND_MESSAGE_V2_VALUE
////////////////////////////////////////////////////////////////////////////////
void SendMessageRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"producerGroup\":\"" << producerGroup << "\","
       << "\"topic\":\"" << topic << "\","
       << "\"defaultTopic\":\"" << defaultTopic << "\","
       << "\"defaultTopicQueueNums\":" << defaultTopicQueueNums << ","
       << "\"queueId\":" << queueId << ","
       << "\"sysFlag\":" << sysFlag << ","
       << "\"bornTimestamp\":" << bornTimestamp << ","
       << "\"flag\":" << flag << ","
       << "\"properties\":\"" << properties << "\","
       << "\"reconsumeTimes\":" << reconsumeTimes
       << "}";

    outData = ss.str();
}

void SendMessageRequestHeaderV2::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"a\":\"" << a << "\","
       << "\"b\":\"" << b << "\","
       << "\"c\":\"" << c << "\","
       << "\"d\":\"" << d << "\","
       << "\"e\":\"" << e << "\","
       << "\"f\":\"" << f << "\","
       << "\"g\":\"" << g << "\","
       << "\"h\":\"" << h << "\","
       << "\"i\":\"" << i << "\","
       << "\"j\":\"" << j << "\""
       << "}";

    outData = ss.str();
}

SendMessageRequestHeader* SendMessageRequestHeaderV2::createSendMessageRequestHeaderV1(
	const SendMessageRequestHeaderV2* v2)
{
    SendMessageRequestHeader* v1 = new SendMessageRequestHeader();
    v1->producerGroup = v2->a;
    v1->topic = v2->b;
    v1->defaultTopic = v2->c;
    v1->defaultTopicQueueNums = v2->d;
    v1->queueId = v2->e;
    v1->sysFlag = v2->f;
    v1->bornTimestamp = v2->g;
    v1->flag = v2->h;
    v1->properties = v2->i;
    v1->reconsumeTimes = v2->j;

    return v1;
}

SendMessageRequestHeaderV2* SendMessageRequestHeaderV2::createSendMessageRequestHeaderV2(
	const SendMessageRequestHeader* v1)
{
    SendMessageRequestHeaderV2* v2 = new SendMessageRequestHeaderV2();
    v2->a = v1->producerGroup;
    v2->b = v1->topic;
    v2->c = v1->defaultTopic;
    v2->d = v1->defaultTopicQueueNums;
    v2->e = v1->queueId;
    v2->f = v1->sysFlag;
    v2->g = v1->bornTimestamp;
    v2->h = v1->flag;
    v2->i = v1->properties;
    v2->j = v1->reconsumeTimes;

    return v2;
}

void SendMessageResponseHeader::encode(std::string& outData)
{
}

CommandCustomHeader* SendMessageResponseHeader::decode(Json::Value& data)
{
    std::string msgId = data["msgId"].asString();
    int queueId = atoi(data["queueId"].asCString());
    long long queueOffset = KPRUtil::str2ll(data["queueOffset"].asCString());

    SendMessageResponseHeader* h = new SendMessageResponseHeader();

    h->msgId = msgId;
    h->queueId = queueId;
    h->queueOffset = queueOffset;

    return h;
}


////////////////////////////////////////////////////////////////////////////////
// PULL_MESSAGE_VALUE
////////////////////////////////////////////////////////////////////////////////
void PullMessageRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"consumerGroup\":\"" << consumerGroup << "\","
       << "\"topic\":\"" << topic << "\","
       << "\"queueId\":\"" << queueId << "\","
       << "\"queueOffset\":\"" << queueOffset << "\","
       << "\"maxMsgNums\":\"" << maxMsgNums << "\","
       << "\"sysFlag\":\"" << sysFlag << "\","
       << "\"commitOffset\":\"" << commitOffset << "\","
       << "\"suspendTimeoutMillis\":\"" << suspendTimeoutMillis << "\","
       << "\"subscription\":\"" << subscription << "\","
       << "\"subVersion\":\"" << subVersion << "\""
       << "}";

    outData = ss.str();
}

void PullMessageResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"suggestWhichBrokerId\":\"" << suggestWhichBrokerId << "\","
        << "\"nextBeginOffset\":\"" << nextBeginOffset << "\","
        << "\"minOffset\":\"" << minOffset << "\","
        << "\"maxOffset\":\"" << maxOffset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* PullMessageResponseHeader::decode(Json::Value& data)
{
    long long suggestWhichBrokerId = KPRUtil::str2ll(data["suggestWhichBrokerId"].asCString());
    long long nextBeginOffset = KPRUtil::str2ll(data["nextBeginOffset"].asCString());
    long long minOffset = KPRUtil::str2ll(data["minOffset"].asCString());
    long long maxOffset = KPRUtil::str2ll(data["maxOffset"].asCString());

    PullMessageResponseHeader* h = new PullMessageResponseHeader();
    h->suggestWhichBrokerId = suggestWhichBrokerId;
    h->nextBeginOffset = nextBeginOffset;
    h->minOffset = minOffset;
    h->maxOffset = maxOffset;

    return h;
}



////////////////////////////////////////////////////////////////////////////////
// GET_CONSUMER_LIST_BY_GROUP_VALUE
////////////////////////////////////////////////////////////////////////////////
void GetConsumerListByGroupRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"consumerGroup\":\"" << consumerGroup << "\""
       << "}";

    outData = ss.str();
}


////////////////////////////////////////////////////////////////////////////////
// CONSUMER_SEND_MSG_BACK_VALUE
////////////////////////////////////////////////////////////////////////////////
void ConsumerSendMsgBackRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;

    ss << "{"
       << "\"offset\":\"" << offset << "\","
       << "\"group\":\"" << group << "\","
       << "\"delayLevel\":\"" << delayLevel << "\""
       << "}";

    outData = ss.str();
}


////////////////////////////////////////////////////////////////////////////////
// QUERY_CONSUMER_OFFSET_VALUE
////////////////////////////////////////////////////////////////////////////////
void QueryConsumerOffsetRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"consumerGroup\":\"" << consumerGroup << "\","
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\""
        << "}";
    outData = ss.str();
}

void QueryConsumerOffsetResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"offset\":\"" << offset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* QueryConsumerOffsetResponseHeader::decode(Json::Value& data)
{
	long long offset = -1;

	if (data.isMember("offset"))
	{
    	offset = KPRUtil::str2ll(data["offset"].asCString());
    }

    QueryConsumerOffsetResponseHeader* h = new QueryConsumerOffsetResponseHeader();
    h->offset = offset;

    return h;
}


////////////////////////////////////////////////////////////////////////////////
// UPDATE_CONSUMER_OFFSET_VALUE
////////////////////////////////////////////////////////////////////////////////
void UpdateConsumerOffsetRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"consumerGroup\":\"" << consumerGroup << "\","
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\","
        << "\"commitOffset\":\"" << commitOffset << "\""
        << "}";
    outData = ss.str();
}


////////////////////////////////////////////////////////////////////////////////
// UNREGISTER_CLIENT_VALUE
////////////////////////////////////////////////////////////////////////////////
void UnregisterClientRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"producerGroup\":\"" << producerGroup << "\","
        << "\"consumerGroup\":\"" << consumerGroup << "\","
        << "\"clientID\":\"" << clientID << "\""
        << "}";
    outData = ss.str();
}


///////////////////////////////////////////////////////////////////////
// VIEW_MESSAGE_BY_ID_VALUE
///////////////////////////////////////////////////////////////////////
void ViewMessageRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"offset\":" << offset
        << "}";
    outData = ss.str();
}


///////////////////////////////////////////////////////////////////////
// SEARCH_OFFSET_BY_TIMESTAMP_VALUE
///////////////////////////////////////////////////////////////////////
void SearchOffsetRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\","
        << "\"timestamp\":\"" << timestamp << "\""
        << "}";
    outData = ss.str();
}

void SearchOffsetResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"offset\":\"" << offset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* SearchOffsetResponseHeader::decode(Json::Value& data)
{
    long long offset = KPRUtil::str2ll(data["offset"].asCString());

    SearchOffsetResponseHeader* h = new SearchOffsetResponseHeader();
    h->offset = offset;

    return h;
}


///////////////////////////////////////////////////////////////////////
// GET_MAX_OFFSET_VALUE
///////////////////////////////////////////////////////////////////////
void GetMaxOffsetRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\""
        << "}";
    outData = ss.str();
}

void GetMaxOffsetResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"offset\":\"" << offset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* GetMaxOffsetResponseHeader::decode(Json::Value& data)
{
    long long offset = KPRUtil::str2ll(data["offset"].asCString());

    GetMaxOffsetResponseHeader* h = new GetMaxOffsetResponseHeader();
    h->offset = offset;

    return h;
}


///////////////////////////////////////////////////////////////////////
// GET_MIN_OFFSET_VALUE
///////////////////////////////////////////////////////////////////////
void GetMinOffsetRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\""
        << "}";
    outData = ss.str();
}

void GetMinOffsetResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"offset\":\"" << offset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* GetMinOffsetResponseHeader::decode(Json::Value& data)
{
    long long offset = KPRUtil::str2ll(data["offset"].asCString());

    GetMinOffsetResponseHeader* h = new GetMinOffsetResponseHeader();
    h->offset = offset;

    return h;
}



///////////////////////////////////////////////////////////////////////
// GET_EARLIEST_MSG_STORETIME_VALUE
///////////////////////////////////////////////////////////////////////
void GetEarliestMsgStoretimeRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"topic\":\"" << topic << "\","
        << "\"queueId\":\"" << queueId << "\""
        << "}";
    outData = ss.str();
}

void GetEarliestMsgStoretimeResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"timestamp\":\"" << timestamp << "\""
        << "}";
    outData = ss.str();
}


CommandCustomHeader* GetEarliestMsgStoretimeResponseHeader::decode(Json::Value& data)
{
    long long timestamp = KPRUtil::str2ll(data["timestamp"].asCString());

    GetEarliestMsgStoretimeResponseHeader* h = new GetEarliestMsgStoretimeResponseHeader();
    h->timestamp = timestamp;

    return h;
}


///////////////////////////////////////////////////////////////////////
// QUERY_MESSAGE_VALUE
///////////////////////////////////////////////////////////////////////
void QueryMessageRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"topic\":\"" << topic << "\","
        << "\"key\":\"" << key << "\","
        << "\"maxNum\":\"" << maxNum << "\","
        << "\"beginTimestamp\":\"" << beginTimestamp << "\","
        << "\"endTimestamp\":\"" << endTimestamp << "\""
        << "}";
    outData = ss.str();
}

void QueryMessageResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"indexLastUpdateTimestamp\":\"" << indexLastUpdateTimestamp << "\","
        << "\"indexLastUpdatePhyoffset\":\"" << indexLastUpdatePhyoffset << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* QueryMessageResponseHeader::decode(Json::Value& data)
{
    long long indexLastUpdateTimestamp = KPRUtil::str2ll(data["indexLastUpdateTimestamp"].asCString());
    long long indexLastUpdatePhyoffset = KPRUtil::str2ll(data["indexLastUpdatePhyoffset"].asCString());

    QueryMessageResponseHeader* h = new QueryMessageResponseHeader();
    h->indexLastUpdateTimestamp = indexLastUpdateTimestamp;
    h->indexLastUpdatePhyoffset = indexLastUpdatePhyoffset;

    return h;
}


///////////////////////////////////////////////////////////////////////
// GET_KV_CONFIG_VALUE
///////////////////////////////////////////////////////////////////////
void GetKVConfigRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"namespace\":\"" << namespace_ << "\","
        << "\"key\":\"" << key << "\""
        << "}";
    outData = ss.str();
}

void GetKVConfigResponseHeader::encode(std::string& outData)
{
	std::stringstream ss;
    ss  << "{"
        << "\"value\":\"" << value << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* GetKVConfigResponseHeader::decode(Json::Value& data)
{
    GetKVConfigResponseHeader* h = new GetKVConfigResponseHeader();
    h->value = data["value"].asString();

    return h;
}


///////////////////////////////////////////////////////////////////////
// NOTIFY_CONSUMER_IDS_CHANGED_VALUE
///////////////////////////////////////////////////////////////////////
void NotifyConsumerIdsChangedRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"consumerGroup\":\"" << consumerGroup << "\""
        << "}";
    outData = ss.str();
}

CommandCustomHeader* NotifyConsumerIdsChangedRequestHeader::decode(Json::Value& data)
{
    NotifyConsumerIdsChangedRequestHeader* h = new NotifyConsumerIdsChangedRequestHeader();
    h->consumerGroup = data["consumerGroup"].asString();

    return h;
}


///////////////////////////////////////////////////////////////////////
// GET_CONSUMER_RUNNING_INFO_VALUE
///////////////////////////////////////////////////////////////////////
void GetConsumerRunningInfoRequestHeader::encode(std::string& outData)
{
    std::stringstream ss;
    ss  << "{"
        << "\"consumerGroup\":\"" << consumerGroup << "\","
        << "\"clientId\":\"" << clientId << "\","
        << "\"jstackEnable\":\"" << jstackEnable << "\","
        << "}";
    outData = ss.str();
}

CommandCustomHeader* GetConsumerRunningInfoRequestHeader::decode(Json::Value& data)
{
    GetConsumerRunningInfoRequestHeader* h = new GetConsumerRunningInfoRequestHeader();
    h->consumerGroup = data["consumerGroup"].asString();
    h->clientId = data["clientId"].asString();
    h->jstackEnable = false;//not support

    return h;
}


}
