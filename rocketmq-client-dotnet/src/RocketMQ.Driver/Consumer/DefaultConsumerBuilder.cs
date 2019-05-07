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
using RocketMQ.Driver.Consumer.Internal;
using RocketMQ.Driver.Interop;

namespace RocketMQ.Driver.Consumer
{
    public class DefaultPushConsumerBuilder : IPushConsumerBuilder
    {
        private HandleRef _handleRef;

        public DefaultPushConsumerBuilder(string groupId)
        {
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
        }
        
        public IPushConsumerBuilder SetPushConsumerGroupId(string groupId)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerNameServerAddress(string nameServerAddress)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerNameServerDomain(string domain)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerThreadCount(int threadCount)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerMessageBatchMaxSize(int batchSize)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerInstanceName(string instanceName)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerSessionCredentials(string accessKey, string secretKey, string channel)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerLogPath(string logPath)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerLogLevel(LogLevel logLevel)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerLogFileNumAndSize(int fileNum, long fileSize)
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

            return this;
        }

        public IPushConsumerBuilder SetPushConsumerMessageModel(MessageModel messageModel)
        {
            var result = PushConsumerWrap.SetPushConsumerMessageModel(this._handleRef, (CMessageModel)messageModel);
            if (result != 0)
            {
                throw new RocketMQConsumerException($"set consumer logFileNumAndSize error. cpp sdk return code {result}");
            }

            return this;
        }

        public IPushConsumer Build()
        {
            if (this._handleRef.Handle == IntPtr.Zero)
            {
                throw new RocketMQConsumerException("consumer ptr is zero.");
            }

            return new DefaultPushConsumer(this._handleRef.Handle);
        }
        
        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        ~DefaultPushConsumerBuilder()
        {
            this.Dispose();
        }
    }
}