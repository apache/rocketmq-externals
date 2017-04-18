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

#include "Common.h"
#include "DefaultMQPushConsumer.h"
using namespace rmq;

volatile long long g_lastCnt = 0;
volatile long long g_totalCnt = 0;
long long g_lastUpdateTime = 0;

static std::string bin2str(const std::string& strBin)
{
	if(strBin.size() == 0)
	{
		return "";
	}

	std::string sOut;
    const char *p = (const char *)strBin.data();
    size_t len = strBin.size();

    char sBuf[255];
    for (size_t i = 0; i < len; ++i, ++p)
	{
        snprintf(sBuf, sizeof(sBuf), "%02x", (unsigned char) *p);
		sOut += sBuf;
    }

    return sOut;
}


class MsgListener : public MessageListenerConcurrently
{
public:
	MsgListener()
	{
		consumeTimes = 0;
	}

	~MsgListener()
	{

	}

	/**
	 * consume messages
	 * ！！！Notice：multi-thread call, need to pay attention to dealing with multi-threaded re-entry problem
	 * @param  msgs    message list
	 * @param  context context for consumer
	 * @return         [CONSUME_SUCCESS- success，RECONSUME_LATER-consume fail and retry later]
	 */
	ConsumeConcurrentlyStatus consumeMessage(std::list<MessageExt*>& msgs,
											ConsumeConcurrentlyContext& context)
	{
		int cnt = __sync_fetch_and_add(&g_totalCnt, 1);
		long long now = MyUtil::getNowMs();
		long long old = g_lastUpdateTime;
		if ((now - old) >= 1000)
		{
			if (__sync_bool_compare_and_swap(&g_lastUpdateTime, old, now))
			{
				long long time = now - old;
				int tps = (int)((g_totalCnt - g_lastCnt) * 1.0 / time * 1000.0);
				g_lastCnt = g_totalCnt;

				MYDEBUG("[consume]msgcount: %lld, TPS: %d\n", g_totalCnt, tps);
			}
		}


		MessageExt* msg = msgs.front();
		long long offset = msg->getQueueOffset();
		std::string maxOffset = msg->getProperty(Message::PROPERTY_MAX_OFFSET);

		long long diff = MyUtil::str2ll(maxOffset.c_str()) - offset;
		if (diff > 100000)
		{
			if (diff % 10000 == 0)
			{
				MYDEBUG("overload, offset:%lld, diff:%lld\n", offset, diff);
			}
			// return CONSUME_SUCCESS;
		}

		std::list<MessageExt*>::iterator it = msgs.begin();
		for (;it != msgs.end();it++)
		{
			MessageExt* me = *it;
			std::string str;
			str.assign(me->getBody(),me->getBodyLen());

			MYLOG("[%d]|%s|%s\n",  cnt, me->toString().c_str(), str.c_str());
		}

		consumeTimes++;

		/*
		if ((consumeTimes % 2) == 0)
		{
			return RECONSUME_LATER;
		}
		else if ((consumeTimes % 3) == 0)
		{
			context.delayLevelWhenNextConsume = 5;
			return RECONSUME_LATER;
		}
		*/

		// context.ackIndex = msgs.size() - 1;
		return CONSUME_SUCCESS;
	}

	int consumeTimes;
};


void Usage(const char* program)
{
	printf("Usage:%s ip:port [-g group] [-t topic] [-w logpath]\n", program);
	printf("\t -g consumer group\n");
	printf("\t -t topic\n");
	printf("\t -w log path\n");
}


int main(int argc, char* argv[])
{
	if (argc < 2)
	{
		Usage(argv[0]);
		return 0;
	}

	std::string namesrv = argv[1];
	std::string group = "cg_test_push_group";
	std::string topic = "topic_test";
	for (int i=2; i< argc; i++)
	{
		if (strcmp(argv[i],"-g")==0)
		{
			if (i+1 < argc)
			{
				group = argv[i+1];
				i++;
			}
			else
			{
				Usage(argv[0]);
				return 0;
			}
		}
		else if (strcmp(argv[i],"-t")==0)
		{
			if (i+1 < argc)
			{
				topic = argv[i+1];
				i++;
			}
			else
			{
				Usage(argv[0]);
				return 0;
			}
		}
		else if (strcmp(argv[i],"-w")==0)
		{
			if (i+1 < argc)
			{
				MyUtil::initLog(argv[i+1]);
				i++;
			}
			else
			{
				Usage(argv[0]);
				return 0;
			}
		}
		else
		{
			Usage(argv[0]);
			return 0;
		}
	}

    // init client api log, here is not necessary, need to debug the api need to be initialized, you can consider comment it
    // Here only the default print warning, error log, the log will be rolling by day, if you need to modify the log level, please set the environment variable, export ROCKETMQ_LOGLEVEL = loglevel
    // The log level is as follows:
    // 0 - close the log
    // 1 - write error log
    // 2 - write error, warning log
    // 3 - write error, warning, info log
    // 4 - write errors, warnings, info, debug logs
	RocketMQUtil::initLog("/tmp/rocketmq_pushconsumer.log");

	RMQ_DEBUG("consumer.new: %s", group.c_str());
	DefaultMQPushConsumer consumer(group);

	RMQ_DEBUG("consumer.setNamesrvAddr: %s", namesrv.c_str());
	consumer.setNamesrvAddr(namesrv);

	RMQ_DEBUG("consumer.setMessageModel: %s", getMessageModelString(CLUSTERING));
	consumer.setMessageModel(CLUSTERING);

	RMQ_DEBUG("consumer.subscribe");
	consumer.subscribe(topic, "*");

	consumer.setConsumeFromWhere(CONSUME_FROM_LAST_OFFSET);

	// Set the number of each consumption message, the default is 1
	// consumer.setConsumeMessageBatchMaxSize(1);

	// The number of consumer thread pool, the default minimum 5, the maximum 25, the proposed set to the same, more stable
	// consumer.setConsumeThreadMin(25);
	// consumer.setConsumeThreadMax(25);

	// Single message consume timeout, default is 15 minutes
	// When the consumption times out, message will be send back to the retry queue and re-delivered
	// consumer.setConsumeTimeout(15);

	RMQ_DEBUG("consumer.registerMessageListener");
	MsgListener* listener = new MsgListener();
	consumer.registerMessageListener(listener);

	RMQ_DEBUG("consumer.start");
	consumer.start();

	while(1)
	{
		if (getchar()=='e'&&getchar()=='x'&&getchar()=='i'&&getchar()=='t')
		{
			break;
		}
		::sleep(1);
	}

	RMQ_DEBUG("consumer.shutdown");
	consumer.shutdown();
	delete listener;

	return 0;
}
