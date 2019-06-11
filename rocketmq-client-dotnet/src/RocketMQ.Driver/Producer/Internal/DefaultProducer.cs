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
using RocketMQ.Driver.Interop;
using QueueSelectorCallback = RocketMQ.Driver.Interop.ProducerWrap.QueueSelectorCallback;

namespace RocketMQ.Driver.Producer.Internal
{
    internal class DefaultProducer : IProducer
    {
        private readonly IProducerNativeMethodsFacade _producerFacade;
        private DiagnosticListener _diagnosticListener;
        
        public HandleRef Handle { get; private set; }
        
        public ProducerOptions Options { get; }

        public DefaultProducer(ProducerOptions options, IntPtr handle, IProducerNativeMethodsFacade producerFacade = null, DiagnosticListener diagnosticListener = null)
        {
            this.Options = options ?? throw new ArgumentNullException(nameof(options));

            if (handle == IntPtr.Zero)
            {
                throw new ArgumentOutOfRangeException(nameof(handle));
            }
            
            this.Handle = new HandleRef(this, handle);
            this._producerFacade = producerFacade ?? new ProducerNativeMethodsFacade();
            this._diagnosticListener = diagnosticListener;
        }

        public bool StartProducer()
        {
            var startResult = this._producerFacade.StartProducer(this.Handle);
            
            if (this._diagnosticListener?.IsEnabled(ConstValues.RocketMQProducerStart) ?? false)
            {
                this._diagnosticListener.Write(ConstValues.RocketMQProducerStart, new {
                    startResult
                });
            }

            return startResult == 0;
        }

        public bool ShutdownProducer()
        {
            var shutdownResult = ProducerWrap.ShutdownProducer(this.Handle);
            
            if (this._diagnosticListener?.IsEnabled(ConstValues.RocketMQProducerStop) ?? false)
            {
                this._diagnosticListener.Write(ConstValues.RocketMQProducerStop, new {
                    shutdownResult
                });
            }

            return shutdownResult == 0;
        }

        public void SetDiagnosticListener(DiagnosticListener diagnosticListener)
        {
            this._diagnosticListener = diagnosticListener;
        }

        public SendResult SendMessageSync(IMessageBuilder builder)
        {
            var message = builder.Build();
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(builder));
            }

            var result = ProducerWrap.SendMessageSync(this.Handle, message, out var sendResult);

            return result == 0
                ? new SendResult {
                    SendStatus = sendResult.sendStatus,
                    Offset = sendResult.offset,
                    MessageId = sendResult.msgId
                }
                : null;
        }

        public SendResult SendMessageOneway(IMessageBuilder builder)
        {
            var message = builder.Build();
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(builder));
            }

            var result = ProducerWrap.SendMessageOneway(this.Handle, message);

            return result == 0
                ? new SendResult {
                    SendStatus = result,
                    Offset = 0,
                    MessageId = string.Empty
                }
                : null;
        }

        public SendResult SendMessageOrderly(IMessageBuilder builder, QueueSelectorCallback callback, int autoRetryTimes = 0, string args = "")
        {
            var message = builder.Build();
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(builder));
            }

            var argsPtr = Marshal.StringToBSTR(args);
            var result = ProducerWrap.SendMessageOrderly(this.Handle, message, callback, argsPtr, autoRetryTimes, out var sendResult);

            return result == 0
                ? new SendResult {
                    SendStatus = sendResult.sendStatus,
                    Offset = sendResult.offset,
                    MessageId = sendResult.msgId
                }
                : null;
        }

        public void Dispose()
        {
            if (this.Handle.Handle != IntPtr.Zero)
            {
                ProducerWrap.DestroyProducer(this.Handle);
                this.Handle = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        ~DefaultProducer()
        {
            this.Dispose();
        }
    }
}
