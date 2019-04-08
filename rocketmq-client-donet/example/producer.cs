/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
using System;
using System.Threading;
using RocketMQ.Interop;

namespace rocketmq_producer_test
{
    class MainClass
    {
        public static void Main(string[] args)
        {
            Console.WriteLine("Start create producer.");
            var producer = ProducerWrap.CreateProducer("xxx");
            if (producer == IntPtr.Zero)
            {
                Console.WriteLine("zero. Oops.");
            }
            Console.WriteLine(producer.ToString());
            Console.WriteLine("end create producer.");
            try
            {
                var setNameServerAddressResult = ProducerWrap.SetProducerNameServerAddress(producer, "47.101.55.250:9876");
                Console.WriteLine("set name server address result:" + setNameServerAddressResult);

                var setProducerLogPathResult = ProducerWrap.SetProducerLogPath(producer, "C:/rocketmq_log.txt");
                Console.WriteLine("set producer log path result:" + setProducerLogPathResult);

                var setLogLevelResult = ProducerWrap.SetProducerLogLevel(producer, CLogLevel.E_LOG_LEVEL_TRACE);
                Console.WriteLine("set producer log level result:" + setLogLevelResult);

                var startResult = ProducerWrap.StartProducer(producer);
                Console.WriteLine("start result:" + startResult);

                while (true)
                {
                    // message
                    var messageIntPtr = MessageWrap.CreateMessage("test");
                    Console.WriteLine("message intptr:" + messageIntPtr.ToString());

                    var setMessageBodyResult = MessageWrap.SetMessageBody(messageIntPtr, "hello" + Guid.NewGuid());
                    Console.WriteLine("set message body result:" + setMessageBodyResult);

                    var setTagResult = MessageWrap.SetMessageTags(messageIntPtr, "tag_test");
                    Console.WriteLine("set message tag result:" + setTagResult.ToString());

                    var sendResult = ProducerWrap.SendMessageSync(producer, messageIntPtr, out CSendResult sendResultStruct);
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResultStruct.msgId.ToString());

                    Thread.Sleep(500);
                }

                var shutdownResult = ProducerWrap.ShutdownProducer(producer);
                Console.WriteLine("shutdown result:" + shutdownResult);

                var destoryResult = ProducerWrap.DestroyProducer(producer);
                Console.WriteLine("destory result:" + destoryResult);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.ToString());
            }
            Console.ReadKey(true);
        }
    }
}
