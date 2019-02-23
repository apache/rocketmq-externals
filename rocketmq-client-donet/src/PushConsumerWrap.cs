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

namespace RocketMQ.Interop
{
    public static class PushConsumerWrap
    {
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr CreatePushConsumer(string groupId);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int DestroyPushConsumer(IntPtr consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int StartPushConsumer(IntPtr consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ShutdownPushConsumer(IntPtr consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPushConsumerGroupID(IntPtr consumer, string groupId);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPushConsumerNameServerAddress(IntPtr consumer, string namesrv);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPushConsumerSessionCredentials(IntPtr consumer, string accessKey, string secretKey, string channel);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int Subscribe(IntPtr consumer, string topic, string expression);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "GetPushConsumerGroupID")]
        internal static extern IntPtr GetPushConsumerGroupIDInternal(IntPtr consumer);

        public static string GetPushConsumerGroupID(IntPtr consumer)
        {
            var ptr = GetPushConsumerGroupIDInternal(consumer);
            if (ptr == IntPtr.Zero) return string.Empty;
            return Marshal.PtrToStringAnsi(ptr);
        }

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int RegisterMessageCallback(
            IntPtr consumer,
            [MarshalAs(UnmanagedType.FunctionPtr)]
            MessageCallBack pCallback
        );

        public delegate int MessageCallBack(IntPtr consumer, IntPtr messageIntPtr);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPushConsumerLogLevel(IntPtr consumer, CLogLevel level);
    }
}
