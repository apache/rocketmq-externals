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
using System.Diagnostics;
using System.Runtime.InteropServices;
using RocketMQ.Driver.Producer.Internal;

namespace RocketMQ.Driver.Producer
{
    internal class DefaultProducerBuilder : IProducerBuilder
    {
        private readonly ProducerOptions _options;
        private readonly DiagnosticListener _diagnosticListener;
        private readonly IProducerNativeMethodsFacade _producerFacade;

        private HandleRef _handleRef;

        public DefaultProducerBuilder(string groupName, IProducerNativeMethodsFacade producerFacade = null, DiagnosticListener diagnosticListener = null)
        {
            if (string.IsNullOrWhiteSpace(groupName))
            {
                throw new ArgumentNullException(nameof(groupName));
            }
            
            this._options = new ProducerOptions {
                GroupName = groupName
            };
            this._producerFacade = producerFacade ?? new ProducerNativeMethodsFacade();
            this._diagnosticListener = diagnosticListener;

            var handle = this._producerFacade.CreateProducer(groupName);

            if (handle == IntPtr.Zero)
            {
                throw new RocketMQProducerException($"create producer error, ptr is {handle}");
            }
            
            this._handleRef = new HandleRef(this, handle);
        }
        
        public IProducerBuilder SetProducerNameServerAddress(string nameServerAddress)
        {
            if (string.IsNullOrWhiteSpace(nameServerAddress))
            {
                throw new ArgumentException(nameof(nameServerAddress));
            }

            this._options.NameServerAddress = nameServerAddress;

            var result = this._producerFacade.SetProducerNameServerAddress(this._handleRef, nameServerAddress);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer nameServerAddress error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerNameServerDomain(string nameServerDomain)
        {
            if (string.IsNullOrWhiteSpace(nameServerDomain))
            {
                throw new ArgumentException(nameof(nameServerDomain));
            }

            this._options.NameServerDomain = nameServerDomain;

            var result = this._producerFacade.SetProducerNameServerDomain(this._handleRef, nameServerDomain);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer nameServerDomain error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerGroupName(string groupName)
        {
            if (string.IsNullOrWhiteSpace(groupName))
            {
                throw new ArgumentException(nameof(groupName));
            }

            this._options.GroupName = groupName;

            var result = this._producerFacade.SetProducerGroupName(this._handleRef, groupName);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer groupName error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerInstanceName(string instanceName)
        {
            if (string.IsNullOrWhiteSpace(instanceName))
            {
                throw new ArgumentException(nameof(instanceName));
            }

            this._options.InstanceName = instanceName;

            var result = this._producerFacade.SetProducerInstanceName(this._handleRef, instanceName);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer instanceName error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerSessionCredentials(string accessKey, string secretKey, string onsChannel)
        {
            if (string.IsNullOrWhiteSpace(accessKey))
            {
                throw new ArgumentException(nameof(accessKey));
            }
            if (string.IsNullOrWhiteSpace(secretKey))
            {
                throw new ArgumentException(nameof(secretKey));
            }
            if (string.IsNullOrWhiteSpace(onsChannel))
            {
                throw new ArgumentException(nameof(onsChannel));
            }

            this._options.AccessKey = accessKey;
            this._options.SecretKey = secretKey;
            this._options.Channel = onsChannel;

            var result = this._producerFacade.SetProducerSessionCredentials(this._handleRef, accessKey, secretKey, onsChannel);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer sessionCredentials error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerLogPath(string logPath)
        {
            if (string.IsNullOrWhiteSpace(logPath))
            {
                throw new ArgumentException(nameof(logPath));
            }

            this._options.LogPath = logPath;

            var result = this._producerFacade.SetProducerLogPath(this._handleRef, logPath);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logPath error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerLogFileNumAndSize(int fileNum, long fileSize)
        {
            if (fileNum <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileNum));
            }

            if (fileSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileSize));
            }

            this._options.LogFileNum = fileNum;
            this._options.LogFileSize = fileSize;
            
            var result = this._producerFacade.SetProducerLogFileNumAndSize(this._handleRef, fileNum, fileSize);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logFileNumAndSize error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerLogLevel(LogLevel logLevel)
        {
            if (logLevel == LogLevel.None)
            {
                throw new ArgumentException(nameof(logLevel));
            }

            this._options.LogLevel = logLevel;
            
            var result = this._producerFacade.SetProducerLogLevel(this._handleRef, logLevel);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logLevel error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerSendMessageTimeout(int timeout)
        {
            if (timeout < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(timeout));
            }

            this._options.SendMessageTimeout = timeout;
            
            var result = this._producerFacade.SetProducerSendMsgTimeout(this._handleRef, timeout);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer sendMessageTimeout error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerCompressLevel(int level)
        {
            if (level < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(level));
            }

            this._options.CompressLevel = level;
            
            var result = this._producerFacade.SetProducerCompressLevel(this._handleRef, level);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer compressLevel error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducerBuilder SetProducerMaxMessageSize(int size)
        {
            if (size < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(size));
            }

            this._options.MaxMessageSize = size;
            
            var result = this._producerFacade.SetProducerMaxMessageSize(this._handleRef, size);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer maxMessageSize error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IProducer Build()
        {
            if (string.IsNullOrWhiteSpace(this._options.GroupName))
            {
                throw new ArgumentNullException(nameof(this._options.GroupName));
            }
            
            var producer = new DefaultProducer(this._options, this._handleRef.Handle, this._producerFacade, this._diagnosticListener);
            this._handleRef = new HandleRef(null, IntPtr.Zero);
            
            return producer;
        }

        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        ~DefaultProducerBuilder()
        {
            this.Dispose();
        }
    }
}
