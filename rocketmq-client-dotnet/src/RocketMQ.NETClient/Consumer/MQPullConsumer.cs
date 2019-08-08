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
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace RocketMQ.Client.Consumer
{
    public class MQPullConsumer : IPullConsumer
    {
        #region default Options

        private HandleRef _handleRef;
        private readonly string _logPath = Environment.CurrentDirectory + Path.DirectorySeparatorChar + "PullConsumer_log.txt";
        private static int _queueSize = 4;
        private IntPtr[] _intPtrs = new IntPtr[_queueSize];
        private CMessageQueue[] _msgs = new CMessageQueue[_queueSize];
        private static Dictionary<MessageQueue, long> OFFSE_TABLE = new Dictionary<MessageQueue, long>();

        #endregion

        #region  Start and shutdown
        public bool StartPullConsumer()
        {

            var result = PullConsumerWrap.StartPullConsumer(this._handleRef);

            return result == 0;
        }

        public bool ShutdownPullConsumer()
        {
            if (this._handleRef.Handle == IntPtr.Zero)
            {
                return false;
            }

            var result = PullConsumerWrap.ShutdownPullConsumer(this._handleRef);

            return result == 0;
        }

        public bool DestroyPullConsumer()
        {
            if (this._handleRef.Handle == IntPtr.Zero)
            {
                return false;
            }

            var destroyResult = PullConsumerWrap.DestroyPullConsumer(this._handleRef);

            return destroyResult == 0;
        }

        #endregion

        #region Constructor

        private void MQPullConsumerInit(string groupId)
        {
            if (string.IsNullOrWhiteSpace(groupId))
            {
                throw new ArgumentNullException(nameof(groupId));
            }

            var handle = PullConsumerWrap.CreatePullConsumer(groupId);

            if (handle == IntPtr.Zero)
            {
                throw new RocketMQConsumerException($"create consumer error, ptr is {handle}");
            }

            this._handleRef = new HandleRef(this, handle);
            this.SetPullConsumerLogPath(this._logPath);


            for (int i = 0; i < _queueSize; i++)
            {
                _intPtrs[i] = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(CMessageQueue)));
                Marshal.StructureToPtr(_msgs[i], _intPtrs[i], true);
            }


        }
        public MQPullConsumer(string groupId)
        {

            this.MQPullConsumerInit(groupId);
        }

        public MQPullConsumer(string groupId, string nameServerAddress)
        {

            this.MQPullConsumerInit(groupId);
            this.SetPullConsumerNameServerAddress(nameServerAddress);
        }

        public MQPullConsumer(string groupId, string nameServerAddress, string logPath, LogLevel logLevel)
        {
            this.MQPullConsumerInit(groupId);
            this.SetPullConsumerNameServerAddress(nameServerAddress);
            this.SetPullConsumerLogPath(logPath);
            this.SetPullConsumerLogLevel(logLevel);
        }

        #endregion

        #region SET OPTIONS

        public void SetPullConsumerGroupId(string groupId)
        {
            if (string.IsNullOrWhiteSpace(groupId))
            {
                throw new ArgumentNullException(nameof(groupId));
            }

            var result = PullConsumerWrap.SetPullConsumerGroupID(this._handleRef, groupId);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer groupId error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerNameServerAddress(string nameServerAddress)
        {
            if (string.IsNullOrWhiteSpace(nameServerAddress))
            {
                throw new ArgumentNullException(nameof(nameServerAddress));
            }

            var result = PullConsumerWrap.SetPullConsumerNameServerAddress(this._handleRef, nameServerAddress);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer nameServerAddress error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerNameServerDomain(string domain)
        {
            if (string.IsNullOrWhiteSpace(domain))
            {
                throw new ArgumentNullException(nameof(domain));
            }

            var result = PullConsumerWrap.SetPullConsumerNameServerDomain(this._handleRef, domain);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer domain error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerSessionCredentials(string accessKey, string secretKey, string channel)
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

            var result = PullConsumerWrap.SetPullConsumerSessionCredentials(this._handleRef, accessKey, secretKey, channel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer sessionCredentials error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerLogPath(string logPath)
        {
            if (string.IsNullOrWhiteSpace(logPath))
            {
                throw new ArgumentNullException(nameof(logPath));
            }

            var result = PullConsumerWrap.SetPullConsumerLogPath(this._handleRef, logPath);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logPath error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerLogLevel(LogLevel logLevel)
        {
            if (logLevel == LogLevel.None)
            {
                throw new ArgumentException(nameof(logLevel));
            }

            var result = PullConsumerWrap.SetPullConsumerLogLevel(this._handleRef, (CLogLevel)logLevel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logLevel error. cpp sdk return code {result}");
            }

            return;
        }

        public void SetPullConsumerLogFileNumAndSize(int fileNum, long fileSize)
        {
            if (fileNum <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileNum));
            }
            if (fileSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileSize));
            }

            var result = PullConsumerWrap.SetPullConsumerLogFileNumAndSize(this._handleRef, fileNum, fileSize);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logFileNumAndSize error. cpp sdk return code {result}");
            }

            return;
        }

        #endregion

        #region Pull Message API
        public CMessageQueue[] FetchSubscriptionMessageQueues(string topic)
        {
            PullConsumerWrap.FetchSubscriptionMessageQueues(this._handleRef, topic, _intPtrs, ref _queueSize);

            CMessageQueue[] messageQueues = new CMessageQueue[_queueSize];
            for (int j = 0; j < _queueSize; j++)
            {
                messageQueues[j] = (CMessageQueue)(Marshal.PtrToStructure((IntPtr)_intPtrs[j], typeof(CMessageQueue)));
            }
            return messageQueues;

        }
        public CPullResult Pull(MessageQueue mq, CMessageQueue msg, string subExpression, long offset, int maxNums)
        {
            CPullResult cPullResult = PullConsumerWrap.Pull(this._handleRef, ref msg, "", GetMessageQueueOffset(mq), 32);

            return cPullResult;
        }

        public static long GetMessageQueueOffset(MessageQueue mq)
        {

            Console.WriteLine(mq.GetHashCode());
            if (OFFSE_TABLE.ContainsKey(mq))
            {

                OFFSE_TABLE.TryGetValue(mq, out long res);
                return res;
            }


            return 0;
        }

        public static void PutMessageQueueOffset(MessageQueue mq, long offset)
        {
            if (OFFSE_TABLE.ContainsKey(mq)) { OFFSE_TABLE[mq] = offset; }
            else OFFSE_TABLE.Add(mq, offset);
        }


        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                PullConsumerWrap.DestroyPullConsumer(this._handleRef);
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        #endregion
    }

}
