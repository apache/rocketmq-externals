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

namespace RocketMQ.Driver.Interop
{
    public static class ProducerWrap
    {
        // init
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr CreateProducer(string groupId);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int StartProducer(HandleRef producer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ShutdownProducer(HandleRef producer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int DestroyProducer(HandleRef producer);

        // set parameters
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerNameServerAddress(HandleRef producer, string nameServer);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerNameServerDomain(HandleRef producer, string domain);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerGroupName(HandleRef producer, string groupName);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerInstanceName(HandleRef producer, string instanceName);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerSessionCredentials(HandleRef producer, string accessKey, string secretKey, string onsChannel);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerLogPath(HandleRef producer, string logPath);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerLogFileNumAndSize(HandleRef producer, int fileNum, long fileSize);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerLogLevel(HandleRef producer, CLogLevel level);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerSendMsgTimeout(HandleRef producer, int timeout);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerCompressLevel(HandleRef producer, int level);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetProducerMaxMessageSize(HandleRef producer, int size);

        //send
        
        [DllImport(ConstValues.RocketMQDriverDllName,CallingConvention = CallingConvention.Cdecl)]
        public static extern int SendMessageSync(HandleRef producer, HandleRef message, [Out]out CSendResult result);
        
        [DllImport(ConstValues.RocketMQDriverDllName,CallingConvention = CallingConvention.Cdecl)]
        public static extern int SendMessageAsync(
            HandleRef producer,
            HandleRef message,
            [MarshalAs(UnmanagedType.FunctionPtr)]
            CSendSuccessCallback cSendSuccessCallback,
            [MarshalAs(UnmanagedType.FunctionPtr)]
            CSendExceptionCallback cSendExceptionCallback
        );

        [DllImport(ConstValues.RocketMQDriverDllName,CallingConvention = CallingConvention.Cdecl)]
        public static extern int SendMessageOneway(HandleRef producer, HandleRef message);

        /// <summary>
        /// 顺序发送消息。
        /// </summary>
        /// <param name="producer"></param>
        /// <param name="message"></param>
        /// <param name="callback"></param>
        /// <param name="arg">不能为 HandleRef.Zero, CPP SDK 做了检查。</param>
        /// <param name="autoRetryTimes"></param>
        /// <param name="result"></param>
        /// <returns></returns>
        [DllImport(ConstValues.RocketMQDriverDllName,CallingConvention = CallingConvention.Cdecl)]
        public static extern int SendMessageOrderly(
            HandleRef producer,
            HandleRef message,
            [MarshalAs(UnmanagedType.FunctionPtr)]
            QueueSelectorCallback callback,
            IntPtr arg,
            int autoRetryTimes,
            [Out]
            out CSendResult result
        );

        public delegate void CSendSuccessCallback(CSendResult result);

        public delegate void CSendExceptionCallback(CMQException e);
        
        public delegate int QueueSelectorCallback(int size, IntPtr message, IntPtr args);
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct CSendResult
    {
        public int sendStatus;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string msgId;

        public long offset;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct CMQException
    {
        public int error;
        
        public int line;
        
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string file;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 512)]
        public string msg;
        
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 128)]
        public string type;
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
