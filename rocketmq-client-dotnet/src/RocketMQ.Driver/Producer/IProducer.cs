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
using QueueSelectorCallback = RocketMQ.Driver.Interop.ProducerWrap.QueueSelectorCallback;

namespace RocketMQ.Driver.Producer
{
    public interface IProducer :  IDisposable
    {
        /// <summary>
        /// 获取 producer 的 native 句柄。
        /// </summary>
        HandleRef Handle { get; }
        
        ProducerOptions Options { get; }
        
        bool StartProducer();

        bool ShutdownProducer();

        void SetDiagnosticListener(DiagnosticListener diagnosticListener);

        SendResult SendMessageSync(IMessageBuilder builder);

        SendResult SendMessageOneway(IMessageBuilder builder);

        SendResult SendMessageOrderly(IMessageBuilder builder, QueueSelectorCallback callback, int autoRetryTimes = 0, string args = "");
    }
}