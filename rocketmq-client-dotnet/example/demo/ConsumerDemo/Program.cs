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


using RocketMQ.Client.Consumer;
using RocketMQ.Client.Interop;
using RocketMQ.Client.Message;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ConsumerDemo
{
    class Program
    {
        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        static void Main(string[] args)
        {
            Console.Title = "PushConsumer";
            Console.WriteLine("start push consumer...");

            // 创建一个Push消费者
      

            Task.Run(() => {

                var consumer = new MQPushConsumer("GID_NET", "127.0.0.1:9876");
                Console.WriteLine($"consumer: {consumer}");
                // 设置日志目录和级别
                consumer.SetPushConsumerLogPath(".\\consumer_log.txt");
                consumer.SetPushConsumerLogLevel(LogLevel.Trace);

                // 获取消费者组号
                var groupId = consumer.GetPushConsumerGroupID();
                Console.WriteLine($"groupId: {groupId}");

                //  订阅一个`topic`
                consumer.Subscribe("MQ_INST_1547778772487337_Ba4IiUHE%NETP", "*");

                //注册回调函数
                consumer.RegisterMessageCallback(_callback);

                //启动消费者
                var result = consumer.StartPushConsumer();
                Console.WriteLine($"start push consumer ptr: {result}");

                while (true)
                {
                    Thread.Sleep(1000);
                }
            });
            Console.ReadKey(true);
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
