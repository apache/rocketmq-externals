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
using System.Text;
using RocketMQ.Driver.Interop;

namespace RocketMQ.Driver.Producer
{
    public class DefaultMessageBuilder : IMessageBuilder
    {
        private HandleRef _handleRef;

        public DefaultMessageBuilder(string topic)
        {
            if (string.IsNullOrWhiteSpace(topic))
            {
                throw new ArgumentException(nameof(topic));
            }
            
            var handle = MessageWrap.CreateMessage(topic);
            this._handleRef = new HandleRef(this, handle);

            var result = MessageWrap.SetMessageTopic(this._handleRef, topic);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message topic error. cpp sdk return code: {result}");
            }
        }

        public IMessageBuilder SetMessageTopic(string topic)
        {
            if (string.IsNullOrWhiteSpace(topic))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(topic));
            }
            
            var result = MessageWrap.SetMessageTopic(this._handleRef, topic);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message topic error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetMessageTags(string tags)
        {
            if (string.IsNullOrWhiteSpace(tags))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(tags));
            }

            var result = MessageWrap.SetMessageTags(this._handleRef, tags);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message tags error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetMessageKeys(string keys)
        {
            if (string.IsNullOrWhiteSpace(keys))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(keys));
            }

            var result = MessageWrap.SetMessageKeys(this._handleRef, keys);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message keys error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetMessageBody(string body)
        {
            if (string.IsNullOrWhiteSpace(body))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(body));
            }

            var result = MessageWrap.SetMessageBody(this._handleRef, body);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message body error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetByteMessageBody(byte[] body)
        {
            if (body == null || body.Length == 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(body));
            }

            var byteBody = Encoding.UTF8.GetString(body);
            var result = MessageWrap.SetByteMessageBody(this._handleRef, byteBody, byteBody.Length);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message body error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetMessageProperty(string key, string value)
        {
            if (string.IsNullOrWhiteSpace(key))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(key));
            }
            if (string.IsNullOrWhiteSpace(value))
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentException(nameof(value));
            }

            var result = MessageWrap.SetMessageProperty(this._handleRef, key, value);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set message property error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public IMessageBuilder SetDelayTimeLevel(int level)
        {
            if (level < 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new ArgumentOutOfRangeException(nameof(level));
            }
            
            var result = MessageWrap.SetDelayTimeLevel(this._handleRef, level);
            if (result != 0)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                throw new Exception($"set delay time level error. cpp sdk return code: {result}");
            }
            
            return this;
        }

        public HandleRef Build()
        {
            return this._handleRef;
        }
        
        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                MessageWrap.DestroyMessage(this._handleRef);
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }

        ~DefaultMessageBuilder()
        {
            this.Dispose();
        }
    }
}
