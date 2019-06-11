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
using System.Threading.Tasks;
using RocketMQ.Driver.Interop;

namespace rocketmq_push_consumer_test
{
    class Program
    {
        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        static void Main(string[] args)
        {
            Console.Title = "PushConsumer";

            Task.Run(() => {
                Console.WriteLine("start push consumer...");

                var consumerPtr = PushConsumerWrap.CreatePushConsumer("xxx");
                var p = new Program();
                var consumer = new HandleRef(p, consumerPtr);
                Console.WriteLine($"consumer: {consumer}");
                var r0 = PushConsumerWrap.SetPushConsumerLogLevel(consumer, CLogLevel.E_LOG_LEVEL_TRACE);

                var groupId = PushConsumerWrap.GetPushConsumerGroupID(consumer.Handle);
                Console.WriteLine($"groupId: {groupId}");

                var r1 = PushConsumerWrap.SetPushConsumerNameServerAddress(consumer, "47.101.55.250:9876");
                var r2 = PushConsumerWrap.Subscribe(consumer, "test", "*");
                var r3 = PushConsumerWrap.RegisterMessageCallback(consumer, _callback);
                var r10 = PushConsumerWrap.StartPushConsumer(consumer);
                Console.WriteLine($"start push consumer ptr: {r10}");

                while (true)
                {
                    Thread.Sleep(500);
                }
            });
            Console.ReadKey(true);

            //PushConsumerBinder.DestroyPushConsumer(consumer);
        }

        public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
        {
            Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");

            var body = MessageWrap.GetMessageBody(message);
            Console.WriteLine($"body: {body}");

            var messageId = MessageWrap.GetMessageId(message);
            Console.WriteLine($"message_id: {messageId}");

            return 0;
        }
    }
}
