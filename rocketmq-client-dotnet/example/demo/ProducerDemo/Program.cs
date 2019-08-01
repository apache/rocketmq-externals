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

using RocketMQ.Client.Message;
using RocketMQ.Client.Producer;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ProducerDemo
{
    class Program
    {
        private static ProducerWrap.QueueSelectorCallback _queueSelectorCallback = new ProducerWrap.QueueSelectorCallback(
           (size, message, args) =>
           {
               Console.WriteLine($"size: {size}, message: {message}, ptr: {args}");

               return 0;
           });

        static void Main(string[] args)
        {
            Console.Title = "Producer";

            Console.WriteLine("Start create producer.");

            MQProducer producer = new MQProducer("GID_NET");
            producer.SetProducerNameServerAddress("127.0.0.1:9876");
            producer.StartProducer();

            try
            {
                while (true)
                {
                    // message
                    MQMessage message = new MQMessage("MQ_INST_1547778772487337_Ba4IiUHE%NETP"); 

                    // SendMessageSync
                    var sendResult = producer.SendMessageSync(message.GetHandleRef());
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResult.MessageId);
                    message.Dispose();

                    // SendMessageOneway
                    //var sendResult = producer.SendMessageOneway(message);
                    //Console.WriteLine("send result:" + sendResult);
                    //message.Dispose();

                    // SendMessageOneWay
                    //var sendResult = producer.SendMessageOrderly(message.GetHandleRef(), _queueSelectorCallback, "aa");
                    //Console.WriteLine("send result:" + sendResult.MessageId);

                    Thread.Sleep(500);
                }
                var shutdownResult = producer.ShutdownProducer();
                Console.WriteLine("shutdown result:" + shutdownResult);

                var destoryResult = producer.DestroyProducer();
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
