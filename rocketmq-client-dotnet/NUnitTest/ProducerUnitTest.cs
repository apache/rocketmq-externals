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

using NUnit.Framework;
using RocketMQ.Client.Consumer;
using RocketMQ.Client.Interop;
using RocketMQ.Client.Message;
using RocketMQ.Client.Producer;
using System;
using System.Threading;
using System.Threading.Tasks;

namespace Tests
{
    public class Tests
    {
        [SetUp]
        public void Setup()
        {
        }

        [Test]
        public void ProducerTest()
        {
             MQProducer producer = new MQProducer("GID_Test");
            Assert.IsNotNull(producer);
            MQProducer producer2 = new MQProducer("GID_Test","127.0.0.1:9876");
            Assert.IsNotNull(producer2);
            MQProducer producer3 = new MQProducer("GID_Test", "127.0.0.1:9876","C:\\log.text",LogLevel.Trace);
            Assert.IsNotNull(producer3);

            //producer.SetProducerGroupName("GID_Test2");
            producer.SetProducerNameServerAddress("127.0.0.1:9876");
            producer.SetProducerSessionCredentials("access key","sercet key","ALIYUN");
            producer.SetProducerGroupName("GID_Test");
            producer.SetProducerLogLevel(LogLevel.Debug);
            producer.SetProducerLogFileNumAndSize(4, 80000);
            Assert.IsTrue(producer.StartProducer());

            #region Test Message

            MQMessage message = new MQMessage("NET_Test");
            MQMessage message2 = new MQMessage("NET_Test","test","TAGB");
            Assert.IsNotNull(message2);
            Assert.IsNotNull(message);
            Assert.IsNotNull(message.GetHandleRef());
            message.SetMessageTags("UnitTest");
            message.SetMessageBody("test");
            message.SetMessageTopic("NET_Test");

            #endregion
            // SendMessageSync
            var sendResult = producer.SendMessageSync(message.GetHandleRef());
            Assert.IsNotNull(sendResult);
            Assert.AreEqual(sendResult.SendStatus, 0);
            Assert.IsNotNull(sendResult.MessageId);

            Assert.IsTrue(producer.DestroyProducer());
            Assert.IsTrue(producer2.DestroyProducer());
            Assert.IsTrue(producer3.DestroyProducer());
            //Assert.Pass();

        }

        [Test]
        public void PushConsumerTest() {
            Task.Run(() =>
            {

                var consumer = new MQPushConsumer("GID_NET", "127.0.0.1:9876");
                var consumer2 = new MQPushConsumer("GID_NET");
                var consumer3 = new MQPushConsumer("GID_NET", "127.0.0.1:9876", "C:\\log.test", LogLevel.Debug);
                Assert.IsNotNull(consumer);
                Assert.IsNotNull(consumer2);
                Assert.IsNotNull(consumer3);
                // 设置日志目录和级别
                consumer.SetPushConsumerLogPath(".\\consumer_log.txt");
                consumer.SetPushConsumerLogLevel(LogLevel.Trace);
                consumer.SetPushConsumerThreadCount(4);
                // 获取消费者组号
                var groupId = consumer.GetPushConsumerGroupID();
                Assert.IsNotNull(groupId);

                //  订阅一个`topic`
                consumer.Subscribe("MQ_INST_1547778772487337_Ba4IiUHE%NETP", "*");

                //注册回调函数
                Assert.IsTrue(consumer.RegisterMessageCallback(_callback));

                //启动消费者
                var result = consumer.StartPushConsumer();
                Assert.IsTrue(result);
                while (true)
                {
                    Thread.Sleep(1000);
                }
            });



        }
        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
        {
            

            var body = MessageWrap.GetMessageBody(message);
            Assert.IsNotNull(body);
            var messageId = MessageWrap.GetMessageId(message);

            Assert.IsNotNull(messageId);

            return 0;
        }
    }
}