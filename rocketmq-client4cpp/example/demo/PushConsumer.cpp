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
	 * 消息消费函数
	 * ！！！注意：此消费函数会被多线程调用，需要注意处理多线程重入问题
	 * @param  msgs    [消息列表]
	 * @param  context [消费上行文]
	 * @return         [CONSUME_SUCCESS-消费成功，RECONSUME_LATER-消费失败稍后重试]
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
			MYDEBUG("overload, offset:%lld, diff:%lld\n", offset, diff);
			// ！！此处开启需谨慎，跳过堆积的消息将会导致堆积的消息永远不会再被消费
			// 超过10w立马返回消费成功，快速略过，避免消息继续堆积
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
			// 表示消费失败，稍后重新消费这批消息
			return RECONSUME_LATER;
		}
		else if ((consumeTimes % 3) == 0)
		{
			// 设置重新消费延时时间，如果用户没有设置，服务器会根据重试次数自动叠加延时时间，建议不用设置
			context.delayLevelWhenNextConsume = 5;
			// 表示消费失败，稍后重新消费这批消息
			return RECONSUME_LATER;
		}
		*/

		// 消费成功或者部分成功，返回CONSUME_SUCCESS
		// context.ackIndex: 标记成功消费到哪里了
		// 默认情况每次消费消息的数量=1，这时默认 context.ackIndex=0，标明偏移量1的消息消费成功
		// 如果设置的单次消费消息的数量>1，则需要设置ackIndex为消费成功的偏移量
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

    // 初始化client api日志，此处非必要，需要对api进行调试才需要进行初始化，可以考虑注释
	// 这里默认只打印警告、错误日志，日志会按天滚动，如果需要修改日志级别，请设置一下环境变量，export ROCKETMQ_LOGLEVEL=日志级别
    // 日志级别如下:
    //  0   - 关闭日志
    //  1   - 写错误 日志
    //  2   - 写错误,警告 日志
    //  3   - 写错误,警告,信息 日志
    //  4   - 写错误,警告,信息,调试 日志
	RocketMQUtil::initLog("/tmp/rocketmq_pushconsumer.log");

	// 初始化RocketMQ消费者，传入消费组名称
	RMQ_DEBUG("consumer.new: %s", group.c_str());
	DefaultMQPushConsumer consumer(group);

	// 设置MQ的NameServer地址
	RMQ_DEBUG("consumer.setNamesrvAddr: %s", namesrv.c_str());
	consumer.setNamesrvAddr(namesrv);

	// 设置消费模式，CLUSTERING-集群模式，BROADCASTING-广播模式
	RMQ_DEBUG("consumer.setMessageModel: %s", getMessageModelString(CLUSTERING));
	consumer.setMessageModel(CLUSTERING);

	// 订阅指定topic下，所有tag的消息，tag相当于消息的二级分类，用于消息二次过滤，避免不关心的消息传输
	RMQ_DEBUG("consumer.subscribe");
	consumer.subscribe(topic, "*");

	// 设置程序第一次时，会从队列哪里开始进行消费
	// CONSUME_FROM_LAST_OFFSET, 如果是第一次启动则从最大位点开始消费，后续启动都从上次记录的位点开始消费，建议在生产环境使用
	// CONSUME_FROM_FIRST_OFFSET，一个新的订阅组第一次启动从队列的最前位置开始消费,后续再启动接着上次消费的进度开始消费
	// CONSUME_FROM_TIMESTAMP， 一个新的订阅组第一次启动从指定时间点开始消费，后续再启动接着上次消费的进度开始消费，时间点设置参见DefaultMQPushConsumer.consumeTimestamp参数
	consumer.setConsumeFromWhere(CONSUME_FROM_LAST_OFFSET);

	// 设置每次消费消息的数量，默认设置是1条
	// consumer.setConsumeMessageBatchMaxSize(1);

	// 消费线程池数量，默认最小5，最大25，建议设置为一样，比较稳定
	// consumer.setConsumeThreadMin(25);
	// consumer.setConsumeThreadMax(25);

	// 单消息消费超时时长，默认为15分钟
	// 消费超时，则对应的消息将会被被放回重试队列，重新进行投递
	// consumer.setConsumeTimeout(15);

	// 注册消费者的消息监听函数
	RMQ_DEBUG("consumer.registerMessageListener");
	MsgListener* listener = new MsgListener();
	consumer.registerMessageListener(listener);

	// 启动消费者
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

	// 停止消费者
	RMQ_DEBUG("consumer.shutdown");
	consumer.shutdown();
	delete listener;

	return 0;
}
