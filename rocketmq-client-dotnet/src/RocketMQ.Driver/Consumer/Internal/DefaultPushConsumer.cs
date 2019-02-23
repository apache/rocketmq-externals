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

namespace RocketMQ.Driver.Consumer.Internal
{
    internal class DefaultPushConsumer : IPushConsumer, IDisposable
    {
        private HandleRef _handleRef;
        
        public DefaultPushConsumer(IntPtr handle)
        {
            this._handleRef = new HandleRef(this, handle);
        }

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

        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                PushConsumerWrap.DestroyPushConsumer(this._handleRef);
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        ~DefaultPushConsumer()
        {
            this.Dispose();
        }
    }
}