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
    public static class ProducerWrap
    {
        // init
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr CreateProducer(string groupId);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int StartProducer(IntPtr producer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ShutdownProducer(IntPtr producer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int DestroyProducer(IntPtr producer);

        // set parameters
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerNameServerAddress(IntPtr producer, string nameServer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerLogPath(IntPtr producer, string logPath);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerLogLevel(IntPtr producer, CLogLevel level);

        //send
        [DllImport(ConstValues.RocketMQDriverDllName,CallingConvention = CallingConvention.Cdecl)]
        public static extern int SendMessageSync(IntPtr producer, IntPtr message, [Out]out CSendResult result);
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct CSendResult
    {
        public int sendStatus;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string msgId;

        public long offset;
    }

    public enum CLogLevel
    {
        E_LOG_LEVEL_FATAL = 1,
        E_LOG_LEVEL_ERROR = 2,
        E_LOG_LEVEL_WARN = 3,
        E_LOG_LEVEL_INFO = 4,
        E_LOG_LEVEL_DEBUG = 5,
        E_LOG_LEVEL_TRACE = 6,
        E_LOG_LEVEL_LEVEL_NUM = 7
    }
}
