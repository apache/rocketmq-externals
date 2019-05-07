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

namespace RocketMQ.Driver.Producer
{
    public interface IProducerBuilder : IDisposable
    {
        IProducerBuilder SetProducerNameServerAddress(string nameServerAddress);

        IProducerBuilder SetProducerNameServerDomain(string nameServerDomain);

        IProducerBuilder SetProducerGroupName(string groupName);

        IProducerBuilder SetProducerInstanceName(string instanceName);

        IProducerBuilder SetProducerSessionCredentials(string accessKey, string secretKey, string onsChannel);

        IProducerBuilder SetProducerLogPath(string logPath);

        IProducerBuilder SetProducerLogFileNumAndSize(int fileNum, long fileSize);

        IProducerBuilder SetProducerLogLevel(LogLevel level);

        IProducerBuilder SetProducerSendMessageTimeout(int timeout);

        IProducerBuilder SetProducerCompressLevel(int level);

        IProducerBuilder SetProducerMaxMessageSize(int size);

        IProducer Build();
    }

    public class ProducerOptions
    {
        public string NameServerAddress { get; internal set; }

        public string NameServerDomain { get; internal set; }

        public string GroupName { get; internal set; }

        public string InstanceName { get; internal set; }

        public string AccessKey { get; internal set; }

        public string SecretKey { get; internal set; }

        public string Channel { get; internal set; }

        public string LogPath { get; internal set; }

        public int LogFileNum { get; internal set; }

        public long LogFileSize { get; internal set; }

        public LogLevel LogLevel { get; internal set; }

        public int SendMessageTimeout { get; internal set; }

        public int CompressLevel { get; internal set; }

        public int MaxMessageSize { get; internal set; }
    }
}
