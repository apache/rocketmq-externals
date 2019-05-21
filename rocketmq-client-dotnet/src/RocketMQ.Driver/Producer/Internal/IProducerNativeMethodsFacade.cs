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

namespace RocketMQ.Driver.Producer.Internal
{
    public interface IProducerNativeMethodsFacade
    {
        // sets
        IntPtr CreateProducer(string groupId);
        
        int StartProducer(HandleRef producer);
        
        int SetProducerNameServerAddress(HandleRef producer, string nameServer);
        
        int SetProducerNameServerDomain(HandleRef producer, string domain);
        
        int SetProducerGroupName(HandleRef producer, string groupName);
        
        int SetProducerInstanceName(HandleRef producer, string instanceName);
        
        int SetProducerSessionCredentials(HandleRef producer, string accessKey, string secretKey, string onsChannel);
        
        int SetProducerLogPath(HandleRef producer, string logPath);
        
        int SetProducerLogFileNumAndSize(HandleRef producer, int fileNum, long fileSize);
        
        int SetProducerLogLevel(HandleRef producer, LogLevel level);
        
        int SetProducerSendMsgTimeout(HandleRef producer, int timeout);
        
        int SetProducerCompressLevel(HandleRef producer, int level);
        
        int SetProducerMaxMessageSize(HandleRef producer, int size);
    }
}