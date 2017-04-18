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
#include "DefaultMQProducer.h"
using namespace rmq;

void Usage(const char* program)
{
	printf("Usage:%s ip:port [-g group] [-t topic] [-n count] [-s size] [-w logpath]\n", program);
	printf("\t -g group\n");
	printf("\t -t topic\n");
	printf("\t -n message count\n");
	printf("\t -s message size \n");
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
	std::string group = "pg_test_group";
	std::string topic = "topic_test";
	int size = 32;
	int count = 1000;

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
		else if (strcmp(argv[i],"-n")==0)
		{
			if (i+1 < argc)
			{
				count = atoi(argv[i+1]);
				i++;
			}
			else
			{
				Usage(argv[0]);
				return 0;
			}
		}
		else if (strcmp(argv[i],"-s")==0)
		{
			if (i+1 < argc)
			{
				size = atoi(argv[i+1]);
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
	RocketMQUtil::initLog("/tmp/rocketmq_producer.log");

	RMQ_DEBUG("producer.new: %s", group.c_str());
	DefaultMQProducer producer(group);

	RMQ_DEBUG("producer.setNamesrvAddr: %s", namesrv.c_str());
	producer.setNamesrvAddr(namesrv);

	RMQ_DEBUG("producer.start");
	producer.start();

	std::string tags[] = { "TagA", "TagB", "TagC", "TagD", "TagE" };

	int _cost = 0, _tps = 0, _avg = 0, _min = 0, _max = 0;
	int _failCnt = 0;
	TimeCount tc;
	TimeCount tcTotal;
	tcTotal.begin();

	int nNow = time(NULL);
	char key[64];
	char value[1024];

	std::string str;
	for (int i = 0; i < size; i += 8)
	{
        str.append("hello baby");
    }

	for (int i = 0; i < count; i++)
	{
		try
		{

			tc.begin();

			snprintf(key, sizeof(key), "KEY_%d_%d", nNow, i);
			snprintf(value, sizeof(value), "%011d_%s", i, str.c_str());
			Message msg(topic,// topic
				tags[i % 5],// tag
				key,// key
				value,// body
				strlen(value)+1
			);

			// Send messages synchronously
			SendResult sendResult = producer.send(msg);

			tc.end();

			int cost = tc.countMsec();
			_min = (_min == 0) ? cost : (std::min(cost, _min));
			_max = (_max == 0) ? cost : (std::max(cost, _max));

			MYLOG("[%d]|succ|cost:%dms, result:%s\n", i, cost, sendResult.toString().c_str());
		}
		catch (MQClientException& e)
		{
			_failCnt++;
			MYLOG("[%d]|fail|%s\n", i, e.what());
		}
	}
	tcTotal.end();

	MYDEBUG("statsics: num=%d, fail=%d, total_cost=%ds, tps=%d, avg=%dms, min=%dms, max=%dms\n",
		count, _failCnt, tcTotal.countSec(), (int)((double)count/(tcTotal.countMsec()/1000)),
		tcTotal.countMsec()/count, _min, _max);

	// 停止生产者
	producer.shutdown();

	return 0;
}

