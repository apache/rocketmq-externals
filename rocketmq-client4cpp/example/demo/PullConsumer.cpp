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

	// 初始化client api日志，此处非必要，需要对api进行调试才需要进行初始化，可以考虑注释
	// 这里默认只打印警告、错误日志，日志会按天滚动，如果需要修改日志级别，请设置一下环境变量，export ROCKETMQ_LOGLEVEL=日志级别
    // 日志级别如下:
    //  0   - 关闭日志
    //  1   - 写错误 日志
    //  2   - 写错误,警告 日志
    //  3   - 写错误,警告,信息 日志
    //  4   - 写错误,警告,信息,调试 日志
	RocketMQUtil::initLog("/tmp/rocketmq_pullconsumer.log");

	// 初始化RocketMQ消费者，传入消费组名称
	RMQ_DEBUG("consumer.new: %s", group.c_str());
	DefaultMQPullConsumer consumer(group);

	// 设置MQ的NameServer地址
	RMQ_DEBUG("consumer.setNamesrvAddr: %s", namesrv.c_str());
	consumer.setNamesrvAddr(namesrv);

	// 设置消费模式，CLUSTERING-集群模式，BROADCASTING-广播模式
	RMQ_DEBUG("consumer.setMessageModel: %s", getMessageModelString(CLUSTERING));
	consumer.setMessageModel(CLUSTERING);

	// 非阻塞模式，拉取超时时间，默认10s
	consumer.setConsumerPullTimeoutMillis(4000);
	// 长轮询模式，Consumer连接在Broker挂起最长时间，默认20s
	consumer.setBrokerSuspendMaxTimeMillis(3000);
	// 长轮询模式，拉取超时时间，默认30s
	consumer.setConsumerTimeoutMillisWhenSuspend(5000);

	// 启动消费者
	RMQ_DEBUG("consumer.start");
	consumer.start();

	// 获取指定topic的路由信息
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
				// 获取消费偏移量
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

				// 拉取消息
				RMQ_DEBUG("consumer.pullBlockIfNotFound");
				//PullResult* pullResult = consumer.pullBlockIfNotFound(mq, "*", offset, 32);
				PullResult* pullResult = consumer.pull(mq, "*", offset, 32);
				PrintResult(*pullResult);

				// 存储Offset，客户端每隔5s会定时刷新到Broker
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

	// 停止消费者
	RMQ_DEBUG("consumer.shutdown");
	consumer.shutdown();

	return 0;
}
