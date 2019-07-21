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

using RocketMQ.NetClient.Consumer;
using RocketMQ.NetClient.Interop;
using RocketMQ.NetClient.Message;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace RocketMQ.NETClient.Consumer
{
    public class MQPushConsumer : IPushConsumer
    {
        #region default Options
        private HandleRef _handleRef;
        private readonly string LogPath = Environment.CurrentDirectory.ToString() + "\\pushConsumer_log.txt";

        #endregion

        #region Constructor

        private void MQPushConsumerInit(string groupId) {
            if (string.IsNullOrWhiteSpace(groupId))
            {
                throw new ArgumentNullException(nameof(groupId));
            }

            var handle = PushConsumerWrap.CreatePushConsumer(groupId);

            if (handle == IntPtr.Zero)
            {
                throw new RocketMQConsumerException($"create consumer error, ptr is {handle}");
            }

            this._handleRef = new HandleRef(this, handle);
            this.SetPushConsumerLogPath(this.LogPath);
        }
        public MQPushConsumer(string groupId) {

            this.MQPushConsumerInit(groupId);
        }

        public MQPushConsumer(string groupId, string nameServerAddress)
        {

            this.MQPushConsumerInit(groupId);
            this.SetPushConsumerNameServerAddress(nameServerAddress);
        }

        public MQPushConsumer(string groupId, string nameServerAddress, string logPath, LogLevel logLevel) {
            this.MQPushConsumerInit(groupId);
            this.SetPushConsumerNameServerAddress(nameServerAddress);
            this.SetPushConsumerLogPath(logPath);
            this.SetPushConsumerLogLevel(logLevel);
        }


        #endregion

        #region set options

        public void SetPushConsumerGroupId(string groupId)
        {
            if (string.IsNullOrWhiteSpace(groupId))
            {
                throw new ArgumentNullException(nameof(groupId));
            }

            var result = PushConsumerWrap.SetPushConsumerGroupID(this._handleRef, groupId);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer groupId error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerNameServerAddress(string nameServerAddress)
        {
            if (string.IsNullOrWhiteSpace(nameServerAddress))
            {
                throw new ArgumentNullException(nameof(nameServerAddress));
            }

            var result = PushConsumerWrap.SetPushConsumerNameServerAddress(this._handleRef, nameServerAddress);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer nameServerAddress error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerNameServerDomain(string domain)
        {
            if (string.IsNullOrWhiteSpace(domain))
            {
                throw new ArgumentNullException(nameof(domain));
            }

            var result = PushConsumerWrap.SetPushConsumerNameServerDomain(this._handleRef, domain);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer domain error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerThreadCount(int threadCount)
        {
            if (threadCount <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(threadCount));
            }

            var result = PushConsumerWrap.SetPushConsumerThreadCount(this._handleRef, threadCount);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer threadCount error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerMessageBatchMaxSize(int batchSize)
        {
            if (batchSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(batchSize));
            }

            var result = PushConsumerWrap.SetPushConsumerMessageBatchMaxSize(this._handleRef, batchSize);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer batchSize error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerInstanceName(string instanceName)
        {
            if (string.IsNullOrWhiteSpace(instanceName))
            {
                throw new ArgumentNullException(nameof(instanceName));
            }

            var result = PushConsumerWrap.SetPushConsumerInstanceName(this._handleRef, instanceName);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer instanceName error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerSessionCredentials(string accessKey, string secretKey, string channel)
        {
            if (string.IsNullOrWhiteSpace(accessKey))
            {
                throw new ArgumentNullException(nameof(accessKey));
            }
            if (string.IsNullOrWhiteSpace(secretKey))
            {
                throw new ArgumentNullException(nameof(secretKey));
            }
            if (string.IsNullOrWhiteSpace(channel))
            {
                throw new ArgumentNullException(nameof(channel));
            }

            var result = PushConsumerWrap.SetPushConsumerSessionCredentials(this._handleRef, accessKey, secretKey, channel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer sessionCredentials error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerLogPath(string logPath)
        {
            if (string.IsNullOrWhiteSpace(logPath))
            {
                throw new ArgumentNullException(nameof(logPath));
            }

            var result = PushConsumerWrap.SetPushConsumerLogPath(this._handleRef, logPath);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logPath error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerLogLevel(LogLevel logLevel)
        {
            if (logLevel == LogLevel.None)
            {
                throw new ArgumentException(nameof(logLevel));
            }

            var result = PushConsumerWrap.SetPushConsumerLogLevel(this._handleRef, (CLogLevel)logLevel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logLevel error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerLogFileNumAndSize(int fileNum, long fileSize)
        {
            if (fileNum <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileNum));
            }
            if (fileSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileSize));
            }

            var result = PushConsumerWrap.SetPushConsumerLogFileNumAndSize(this._handleRef, fileNum, fileSize);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logFileNumAndSize error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPushConsumerMessageModel(MessageModel messageModel)
        {
            var result = PushConsumerWrap.SetPushConsumerMessageModel(this._handleRef, (CMessageModel)messageModel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logFileNumAndSize error. cpp sdk return code {result}");
            }

            return;
        }
        #endregion

        #region Start and shutDown
        public bool StartPushConsumer()
        {
            var startResult = PushConsumerWrap.StartPushConsumer(this._handleRef);

            return startResult == 0;
        }

        public bool ShutdownPushConsumer()
        {
            if (this._handleRef.Handle == IntPtr.Zero)
            {
                return false;
            }

            var shutdownResult = PushConsumerWrap.ShutdownPushConsumer(this._handleRef);

            return shutdownResult == 0;
        }
        public bool DestroyPushConsumer()
        {
            if (this._handleRef.Handle == IntPtr.Zero)
            {
                return false;
            }

            var destroyResult = PushConsumerWrap.DestroyPushConsumer(this._handleRef);

            return destroyResult == 0;
        }

        #endregion



        #region Push Message API

        public void Subscribe(string topic, string expression)
        {
            if (string.IsNullOrWhiteSpace(topic))
            {
                throw new ArgumentNullException(nameof(topic));
            }

            if (string.IsNullOrWhiteSpace(expression))
            {
                throw new ArgumentNullException(nameof(expression));
            }

            var result = PushConsumerWrap.Subscribe(this._handleRef, topic, expression);

            if (result != 0)
            {
                throw new RocketMQConsumerException($"push consumer subscribe error. cpp sdk return code: {result}");
            }
        }

        public bool RegisterMessageCallback(PushConsumerWrap.MessageCallBack callBack)
        {
            if (callBack == null)
            {
                return false;
            }

            var registerCallbackResult = PushConsumerWrap.RegisterMessageCallback(this._handleRef, callBack);

            return registerCallbackResult == 0;
        }

        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(
            (consumer, message) => {
                Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");

                var body = MessageWrap.GetMessageBody(message);
                Console.WriteLine($"body: {body}");

                var messageId = MessageWrap.GetMessageId(message);
                Console.WriteLine($"message_id: {messageId}");

                return 0;
            }
            );
        #endregion

        #region GET CONSUMER API

        public string GetPushConsumerGroupID() {
            return PushConsumerWrap.GetPushConsumerGroupID(this._handleRef.Handle);
        }


        #endregion
        #region Dispose
        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                PushConsumerWrap.DestroyPushConsumer(this._handleRef);
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }
        #endregion
        
    }
}
