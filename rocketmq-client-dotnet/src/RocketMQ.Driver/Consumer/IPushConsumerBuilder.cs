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

namespace RocketMQ.Driver.Consumer
{
    public interface IPushConsumerBuilder : IDisposable
    {
        IPushConsumerBuilder SetPushConsumerGroupId(string groupId);

        IPushConsumerBuilder SetPushConsumerNameServerAddress(string nameServerAddress);

        IPushConsumerBuilder SetPushConsumerNameServerDomain(string domain);

        IPushConsumerBuilder SetPushConsumerThreadCount(int threadCount);

        IPushConsumerBuilder SetPushConsumerMessageBatchMaxSize(int batchSize);

        IPushConsumerBuilder SetPushConsumerInstanceName(string instanceName);

        IPushConsumerBuilder SetPushConsumerSessionCredentials(string accessKey, string secretKey, string channel);

        IPushConsumerBuilder SetPushConsumerLogPath(string logPath);

        IPushConsumerBuilder SetPushConsumerLogLevel(LogLevel level);

        IPushConsumerBuilder SetPushConsumerLogFileNumAndSize(int fileNum, long fileSize);

        IPushConsumerBuilder SetPushConsumerMessageModel(MessageModel messageModel);

        IPushConsumer Build();
    }
}
