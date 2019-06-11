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
using System.Runtime.InteropServices;
using System.Threading;
using RocketMQ.Driver.Interop;

namespace rocketmq_producer_test
{
    class MainClass
    {
        private static ProducerWrap.QueueSelectorCallback _queueSelectorCallback = new ProducerWrap.QueueSelectorCallback(
            (size, message, args) =>
            {
                Console.WriteLine($"size: {size}, message: {message}, ptr: {args}");
                
                return 0;
            });
        
        public static void Main(string[] args)
        {
            Console.Title = "Producer";

            Console.WriteLine("Start create producer.");
            var producerPtr = ProducerWrap.CreateProducer("xxx");
            if (producerPtr == IntPtr.Zero)
            {
                Console.WriteLine("zero. Oops.");
            }

            Console.WriteLine(producerPtr.ToString());
            Console.WriteLine("end create producer.");

            var p = new MainClass();
            var producer = new HandleRef(p, producerPtr);
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
                    var message = MessageWrap.CreateMessage("test");
                    Console.WriteLine("message intPtr:" + message);

                    var p1 = new MainClass();
                    var messageIntPtr = new HandleRef(p1, message);

                    var setMessageBodyResult = MessageWrap.SetMessageBody(messageIntPtr, "hello" + Guid.NewGuid());
                    Console.WriteLine("set message body result:" + setMessageBodyResult);

                    var setTagResult = MessageWrap.SetMessageTags(messageIntPtr, "tag_test");
                    Console.WriteLine("set message tag result:" + setTagResult);

                    var setPropertyResult = MessageWrap.SetMessageProperty(messageIntPtr, "key1", "value1");
                    Console.WriteLine("set message property result:" + setPropertyResult);

                    // var setByteMessageBodyResult = MessageWrap.SetByteMessageBody(messageIntPtr, "byte_body", 9);
                    // Console.WriteLine("set byte message body result:" + setByteMessageBodyResult);
                    

                    // SendMessageSync
                    var sendResult = ProducerWrap.SendMessageSync(producer, messageIntPtr, out CSendResult sendResultStruct);
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResultStruct.msgId.ToString());

                    // SendMessageOneway
                    // var sendResult = ProducerWrap.SendMessageOneway(producer, messageIntPtr);
                    // Console.WriteLine("send result:" + sendResult);
                    
                    // SendMessageAsync
                    // var sendResult = ProducerWrap.SendMessageAsync(
                    //     producer,
                    //     messageIntPtr,
                    //     result =>
                    //     {
                    //         Console.WriteLine($"success_callback_msgId: {result.msgId}");
                    //     },
                    //     ex =>
                    //     {
                    //         Console.WriteLine($"error_callback_msgId: {ex.msg}");
                    //     }
                    // );
                    // Console.WriteLine("send result:" + sendResult);

                    // var pArgs = "args_parameters";
                    // var ptrArgs = Marshal.StringToBSTR(pArgs);
                    // var sendResult = ProducerWrap.SendMessageOrderly(producer, messageIntPtr, _queueSelectorCallback,
                    //     ptrArgs, 1, out var sendResultStruct);
                    // Console.WriteLine($"send result:{sendResult}, sendResultStruct -> msgId: {sendResultStruct.msgId}, status: {sendResultStruct.sendStatus}, offset: {sendResultStruct.offset}");

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
