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
#include "DefaultMQPullConsumer.h"
using namespace rmq;

volatile long long g_totalCnt = 0;

void PrintResult(PullResult& result)
{
	std::list<MessageExt*>::iterator it = result.msgFoundList.begin();
	for (;it!=result.msgFoundList.end();it++)
	{
		MessageExt* me = *it;
		std::string str;
		str.assign(me->getBody(),me->getBodyLen());

		int cnt = __sync_fetch_and_add(&g_totalCnt, 1);
		MYLOG("[%d]|%s|%s\n",  cnt, me->toString().c_str(), str.c_str());
	}
}


void Usage(const char* program)
{
	printf("Usage:%s ip:port [-g group] [-t topic] [-w logpath]\n", program);
	printf("\t -g consumer group\n");
	printf("\t -t topic\n");
	printf("\t -w log path\n");
}


int main(int argc, char* argv[])
{
	if (argc<2)
	{
		Usage(argv[0]);
		return 0;
	}

	std::string namesrv = argv[1];
	std::string group = "cg_test_pull_group";
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
	RocketMQUtil::initLog("/tmp/rocketmq_pullconsumer.log");

	RMQ_DEBUG("consumer.new: %s", group.c_str());
	DefaultMQPullConsumer consumer(group);

	RMQ_DEBUG("consumer.setNamesrvAddr: %s", namesrv.c_str());
	consumer.setNamesrvAddr(namesrv);

	RMQ_DEBUG("consumer.setMessageModel: %s", getMessageModelString(CLUSTERING));
	consumer.setMessageModel(CLUSTERING);

	consumer.setConsumerPullTimeoutMillis(4000);
	consumer.setBrokerSuspendMaxTimeMillis(3000);
	consumer.setConsumerTimeoutMillisWhenSuspend(5000);

	RMQ_DEBUG("consumer.start");
	consumer.start();

	RMQ_DEBUG("consumer.fetchSubscribeMessageQueues");
	std::set<MessageQueue>* mqs = consumer.fetchSubscribeMessageQueues(topic);

	std::set<MessageQueue>::iterator it = mqs->begin();
	for (; it!=mqs->end(); it++)
	{
		MessageQueue mq = *it;
		bool noNewMsg = false;
		while (!noNewMsg)
		{
			try
			{
				RMQ_DEBUG("consumer.fetchConsumeOffset");
                long long offset = consumer.fetchConsumeOffset(mq, false);
                if (offset < 0)
                {
                    offset = consumer.maxOffset(mq);
                    if (offset < 0)
                    {
						offset = LLONG_MAX;
                    }
                }

				RMQ_DEBUG("consumer.pullBlockIfNotFound");
				//PullResult* pullResult = consumer.pullBlockIfNotFound(mq, "*", offset, 32);
				PullResult* pullResult = consumer.pull(mq, "*", offset, 32);
				PrintResult(*pullResult);

				RMQ_DEBUG("consumer.updateConsumeOffset");
                consumer.updateConsumeOffset(mq, pullResult->nextBeginOffset);

				switch (pullResult->pullStatus)
				{
					case FOUND:
						// TODO
						break;
					case NO_MATCHED_MSG:
						break;
					case NO_NEW_MSG:
						noNewMsg = true;
						break;
					case OFFSET_ILLEGAL:
						break;
					default:
						break;
				}

				delete pullResult;
			}
			catch (MQException& e)
			{
				std::cout<<e<<std::endl;
			}
		}
	}
	delete mqs;

	RMQ_DEBUG("consumer.shutdown");
	consumer.shutdown();

	return 0;
}
