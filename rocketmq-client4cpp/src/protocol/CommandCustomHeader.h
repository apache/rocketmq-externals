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

#ifndef __COMMANDCUSTOMHEADER_H__
#define __COMMANDCUSTOMHEADER_H__

#include <string>
#include <json/json.h>

namespace rmq
{
    /**
    * RemotingCommand custom header
    *
    */
    class CommandCustomHeader
    {
    public :
        virtual ~CommandCustomHeader() {}
        virtual void encode(std::string& outData) = 0;
        static CommandCustomHeader* decode(int code, Json::Value& data, bool isResponseType);
    };

	///////////////////////////////////////////////////////////////////////
	// GET_ROUTEINTO_BY_TOPIC_VALUE
	///////////////////////////////////////////////////////////////////////
    class GetRouteInfoRequestHeader : public CommandCustomHeader
    {
    public:
        GetRouteInfoRequestHeader()
		{
		};
        ~GetRouteInfoRequestHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
    };

	///////////////////////////////////////////////////////////////////////
	// UPDATE_AND_CREATE_TOPIC_VALUE
	///////////////////////////////////////////////////////////////////////
    class CreateTopicRequestHeader : public CommandCustomHeader
    {
    public:
        CreateTopicRequestHeader()
		{
			readQueueNums = 0;
			writeQueueNums = 0;
			perm = 0;
			topicSysFlag = 0;
			order = false;
		};
        ~CreateTopicRequestHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        std::string defaultTopic;
        int readQueueNums;
        int writeQueueNums;
        int perm;
        std::string topicFilterType;
		int topicSysFlag;
		bool order;
    };

	///////////////////////////////////////////////////////////////////////
	// SEND_MESSAGE_VALUE/SEND_MESSAGE_V2_VALUE
	///////////////////////////////////////////////////////////////////////
    class SendMessageRequestHeader: public CommandCustomHeader
    {
    public:
        SendMessageRequestHeader()
			: defaultTopicQueueNums(0),queueId(0),sysFlag(0),
			bornTimestamp(0),flag(0),reconsumeTimes(0)
		{
		};
        ~SendMessageRequestHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string producerGroup;
        std::string topic;
        std::string defaultTopic;
        int defaultTopicQueueNums;
        int queueId;
        int sysFlag;
        long long bornTimestamp;
        int flag;
        std::string properties;
        int reconsumeTimes;
    };

	class SendMessageRequestHeaderV2: public CommandCustomHeader
	{
	public:
		SendMessageRequestHeaderV2()
			: d(0),e(0),f(0),
			g(0),h(0),j(0)
		{
		};
		~SendMessageRequestHeaderV2() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);
		static SendMessageRequestHeader* createSendMessageRequestHeaderV1(const SendMessageRequestHeaderV2* v2);
		static SendMessageRequestHeaderV2* createSendMessageRequestHeaderV2(const SendMessageRequestHeader* v1);
	public:
		std::string a;	//producerGroup
		std::string b;	//topic
		std::string c;	//defaultTopic
		int d;			//defaultTopicQueueNums
		int e;			//queueId
		int f;			//sysFlag
		long long g;	//bornTimestamp
		int h;			//flag
		std::string i;	//properties
		int j;			//reconsumeTimes
	};

    class SendMessageResponseHeader: public CommandCustomHeader
    {
    public:
        SendMessageResponseHeader()
		{
			queueId = 0;
			queueOffset = 0;
		};
        ~SendMessageResponseHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string msgId;
        int queueId;
        long long queueOffset;
    };


	///////////////////////////////////////////////////////////////////////
	// PULL_MESSAGE_VALUE
	///////////////////////////////////////////////////////////////////////
    class PullMessageRequestHeader: public CommandCustomHeader
    {
    public:
        PullMessageRequestHeader()
		{
			queueId = 0;
			queueOffset = 0;
			maxMsgNums = 0;
			sysFlag = 0;
			commitOffset = 0;
			suspendTimeoutMillis = 0;
			subVersion = 0;
		};
        ~PullMessageRequestHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
        std::string topic;
        int queueId;
        long long queueOffset;
        int maxMsgNums;
        int sysFlag;
        long long commitOffset;
        long long suspendTimeoutMillis;
        std::string subscription;
        long long subVersion;
    };

    class PullMessageResponseHeader: public CommandCustomHeader
    {
    public:
        PullMessageResponseHeader()
		{
			suggestWhichBrokerId = 0;
			nextBeginOffset = 0;
			minOffset = 0;
			maxOffset = 0;
		};
        ~PullMessageResponseHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        long long suggestWhichBrokerId;
        long long nextBeginOffset;
        long long minOffset;
        long long maxOffset;
    };

	///////////////////////////////////////////////////////////////////////
	// GET_CONSUMER_LIST_BY_GROUP_VALUE
	///////////////////////////////////////////////////////////////////////
    class GetConsumerListByGroupRequestHeader : public CommandCustomHeader
    {
    public:
        GetConsumerListByGroupRequestHeader() {};
        ~GetConsumerListByGroupRequestHeader() {};
        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
    };


	///////////////////////////////////////////////////////////////////////
	// CONSUMER_SEND_MSG_BACK_VALUE
	///////////////////////////////////////////////////////////////////////
    class ConsumerSendMsgBackRequestHeader : public CommandCustomHeader
    {
    public:
        ConsumerSendMsgBackRequestHeader()
		{
			offset = 0;
			delayLevel = 0;
		};
        ~ConsumerSendMsgBackRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        long long offset;
        std::string group;
        int delayLevel;
    };


	///////////////////////////////////////////////////////////////////////
	// QUERY_CONSUMER_OFFSET_VALUE
	///////////////////////////////////////////////////////////////////////
    class QueryConsumerOffsetRequestHeader : public CommandCustomHeader
    {
    public:
        QueryConsumerOffsetRequestHeader()
		{
			queueId = 0;
		};
        ~QueryConsumerOffsetRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
        std::string topic;
        int queueId;
    };

    class QueryConsumerOffsetResponseHeader : public CommandCustomHeader
    {
    public:
        QueryConsumerOffsetResponseHeader()
		{
			offset = 0;
		};
        ~QueryConsumerOffsetResponseHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        long long offset;
    };

	///////////////////////////////////////////////////////////////////////
	// UPDATE_CONSUMER_OFFSET_VALUE
	///////////////////////////////////////////////////////////////////////
    class UpdateConsumerOffsetRequestHeader : public CommandCustomHeader
    {
    public:
        UpdateConsumerOffsetRequestHeader()
		{
			queueId = 0;
			commitOffset = 0;
		};
        ~UpdateConsumerOffsetRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
        std::string topic;
        int queueId;
        long long commitOffset;
    };

	///////////////////////////////////////////////////////////////////////
	// UNREGISTER_CLIENT_VALUE
	///////////////////////////////////////////////////////////////////////
    class UnregisterClientRequestHeader : public CommandCustomHeader
    {
    public:
        UnregisterClientRequestHeader() {};
        ~UnregisterClientRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string clientID;
        std::string producerGroup;
        std::string consumerGroup;
    };


	///////////////////////////////////////////////////////////////////////
	// VIEW_MESSAGE_BY_ID_VALUE
	///////////////////////////////////////////////////////////////////////
	class ViewMessageRequestHeader : public CommandCustomHeader
    {
    public:
        ViewMessageRequestHeader()
		{
			offset = 0;
		};
        ~ViewMessageRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        long long offset;
    };

	///////////////////////////////////////////////////////////////////////
	// SEARCH_OFFSET_BY_TIMESTAMP_VALUE
	///////////////////////////////////////////////////////////////////////
	class SearchOffsetRequestHeader : public CommandCustomHeader
    {
    public:
        SearchOffsetRequestHeader()
		{
			queueId = 0;
			timestamp = 0;
		};
        ~SearchOffsetRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        int queueId;
        long long timestamp;
    };

	class SearchOffsetResponseHeader : public CommandCustomHeader
	{
	public:
		SearchOffsetResponseHeader()
		{
			offset = 0;
		};
		~SearchOffsetResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		long long offset;
	};

	///////////////////////////////////////////////////////////////////////
	// GET_MAX_OFFSET_VALUE
	///////////////////////////////////////////////////////////////////////
	class GetMaxOffsetRequestHeader : public CommandCustomHeader
    {
    public:
        GetMaxOffsetRequestHeader()
		{
			queueId = 0;
		};
        ~GetMaxOffsetRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        int queueId;
    };

	class GetMaxOffsetResponseHeader : public CommandCustomHeader
	{
	public:
		GetMaxOffsetResponseHeader()
		{
			offset = 0;
		};
		~GetMaxOffsetResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		long long offset;
	};

	///////////////////////////////////////////////////////////////////////
	// GET_MIN_OFFSET_VALUE
	///////////////////////////////////////////////////////////////////////
	class GetMinOffsetRequestHeader : public CommandCustomHeader
    {
    public:
        GetMinOffsetRequestHeader()
		{
			queueId = 0;
		};
        ~GetMinOffsetRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        int queueId;
    };

	class GetMinOffsetResponseHeader : public CommandCustomHeader
	{
	public:
		GetMinOffsetResponseHeader()
		{
			offset = 0;
		};
		~GetMinOffsetResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		long long offset;
	};


	///////////////////////////////////////////////////////////////////////
	// GET_EARLIEST_MSG_STORETIME_VALUE
	///////////////////////////////////////////////////////////////////////
	class GetEarliestMsgStoretimeRequestHeader : public CommandCustomHeader
    {
    public:
        GetEarliestMsgStoretimeRequestHeader()
		{
			queueId = 0;
		};
        ~GetEarliestMsgStoretimeRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        int queueId;
    };

	class GetEarliestMsgStoretimeResponseHeader : public CommandCustomHeader
	{
	public:
		GetEarliestMsgStoretimeResponseHeader()
		{
			timestamp = 0;
		};
		~GetEarliestMsgStoretimeResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		long long timestamp;
	};

	///////////////////////////////////////////////////////////////////////
	// QUERY_MESSAGE_VALUE
	///////////////////////////////////////////////////////////////////////
	class QueryMessageRequestHeader : public CommandCustomHeader
    {
    public:
        QueryMessageRequestHeader()
		{
			maxNum = 0;
			beginTimestamp = 0;
			endTimestamp = 0;
		};
        ~QueryMessageRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string topic;
        std::string key;
		int maxNum;
		long long beginTimestamp;
		long long endTimestamp;
    };

	class QueryMessageResponseHeader : public CommandCustomHeader
	{
	public:
		QueryMessageResponseHeader()
		{
			indexLastUpdateTimestamp = 0;
			indexLastUpdatePhyoffset = 0;
		};
		~QueryMessageResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		long long indexLastUpdateTimestamp;
		long long indexLastUpdatePhyoffset;
	};

	///////////////////////////////////////////////////////////////////////
	// GET_KV_CONFIG_VALUE
	///////////////////////////////////////////////////////////////////////
	class GetKVConfigRequestHeader : public CommandCustomHeader
    {
    public:
        GetKVConfigRequestHeader() {};
        ~GetKVConfigRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string namespace_;
        std::string key;
    };

	class GetKVConfigResponseHeader : public CommandCustomHeader
	{
	public:
		GetKVConfigResponseHeader() {};
		~GetKVConfigResponseHeader() {};

		virtual void encode(std::string& outData);
		static CommandCustomHeader* decode(Json::Value& data);

	public:
		std::string value;
	};

	///////////////////////////////////////////////////////////////////////
	// NOTIFY_CONSUMER_IDS_CHANGED_VALUE
	///////////////////////////////////////////////////////////////////////
	class NotifyConsumerIdsChangedRequestHeader : public CommandCustomHeader
    {
    public:
        NotifyConsumerIdsChangedRequestHeader() {};
        ~NotifyConsumerIdsChangedRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
    };


	///////////////////////////////////////////////////////////////////////
	// GET_CONSUMER_RUNNING_INFO_VALUE
	///////////////////////////////////////////////////////////////////////
	class GetConsumerRunningInfoRequestHeader : public CommandCustomHeader
    {
    public:
        GetConsumerRunningInfoRequestHeader() {};
        ~GetConsumerRunningInfoRequestHeader() {};

        virtual void encode(std::string& outData);
        static CommandCustomHeader* decode(Json::Value& data);

    public:
        std::string consumerGroup;
		std::string clientId;
		bool jstackEnable;
    };
}

#endif
