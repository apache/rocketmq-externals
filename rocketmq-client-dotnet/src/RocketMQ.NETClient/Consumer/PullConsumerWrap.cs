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

using RocketMQ.NetClient.Interop;
using RocketMQ.NETClient.Consumer;
using System;
using System.Runtime.InteropServices;

namespace RocketMQ.NetClient.Consumer
{
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct CMessageQueue
    {
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 512)]
        public char[] topic;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 256)]
        public char[] brokerName;
        public int queueId;
    };




    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct CPullResult
    {
        public CPullStatus pullStatus;
        public Int64 nextBeginOffset;
        public Int64 minOffset;
        public Int64 maxOffset;
        IntPtr msgFoundList;
        int size;
        IntPtr pData;
    };



    public static class PullConsumerWrap
    {
       

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr CreatePullConsumer(string groupId);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int DestroyPullConsumer(HandleRef consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int StartPullConsumer(HandleRef consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ShutdownPullConsumer(HandleRef consumer);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerGroupID(HandleRef consumer, string groupId);



        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerNameServerAddress(HandleRef consumer, string namesrv);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerNameServerDomain(HandleRef consumer, string domain);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "GetPullConsumerGroupID")]
        internal static extern IntPtr GetPullConsumerGroupIDInternal(IntPtr consumer);

        public static string GetPullConsumerGroupID(IntPtr consumer)
        {
            var ptr = GetPullConsumerGroupIDInternal(consumer);
            if (ptr == IntPtr.Zero) return string.Empty;
            return Marshal.PtrToStringAnsi(ptr);
        }

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerSessionCredentials(HandleRef consumer, string accessKey, string secretKey, string channel);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerLogPath(HandleRef consumer, string logPath);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerLogFileNumAndSize(HandleRef consumer, int fileNum, long fileSize);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetPullConsumerLogLevel(HandleRef consumer, CLogLevel level);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int FetchSubscriptionMessageQueues(HandleRef consumer, string topic, IntPtr[] mqs,ref int size);
   
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ReleaseSubscriptionMessageQueue(ref CMessageQueue mqs);


        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern CPullResult Pull(HandleRef consumer, ref CMessageQueue mq, string subExpression,Int64 offset, int maxNums);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern  int ReleasePullResult(CPullResult pullResult);

        
    }
    

}
