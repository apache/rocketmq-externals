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
using RocketMQ.Driver.Interop;

namespace RocketMQ.Driver.Producer.Internal
{
    internal class ProducerNativeMethodsFacade : IProducerNativeMethodsFacade
    {
        public IntPtr CreateProducer(string groupId)
        {
            return ProducerWrap.CreateProducer(groupId);
        }

        public int StartProducer(HandleRef producer)
        {
            return ProducerWrap.StartProducer(producer);
        }

        public int SetProducerNameServerAddress(HandleRef producer, string nameServer)
        {
            return ProducerWrap.SetProducerNameServerAddress(producer, nameServer);
        }

        public int SetProducerNameServerDomain(HandleRef producer, string domain)
        {
            return ProducerWrap.SetProducerNameServerDomain(producer, domain);
        }

        public int SetProducerGroupName(HandleRef producer, string groupName)
        {
            return ProducerWrap.SetProducerGroupName(producer, groupName);
        }

        public int SetProducerInstanceName(HandleRef producer, string instanceName)
        {
            return ProducerWrap.SetProducerInstanceName(producer, instanceName);
        }

        public int SetProducerSessionCredentials(HandleRef producer, string accessKey, string secretKey, string onsChannel)
        {
            return ProducerWrap.SetProducerSessionCredentials(producer, accessKey, secretKey, onsChannel);
        }

        public int SetProducerLogPath(HandleRef producer, string logPath)
        {
            return ProducerWrap.SetProducerLogPath(producer, logPath);
        }

        public int SetProducerLogFileNumAndSize(HandleRef producer, int fileNum, long fileSize)
        {
            return ProducerWrap.SetProducerLogFileNumAndSize(producer, fileNum, fileSize);
        }

        public int SetProducerLogLevel(HandleRef producer, LogLevel level)
        {
            return ProducerWrap.SetProducerLogLevel(producer, (CLogLevel) level);
        }

        public int SetProducerSendMsgTimeout(HandleRef producer, int timeout)
        {
            return ProducerWrap.SetProducerSendMsgTimeout(producer, timeout);
        }

        public int SetProducerCompressLevel(HandleRef producer, int level)
        {
            return ProducerWrap.SetProducerCompressLevel(producer, level);
        }

        public int SetProducerMaxMessageSize(HandleRef producer, int size)
        {
            return ProducerWrap.SetProducerMaxMessageSize(producer, size);
        }
    }
}
