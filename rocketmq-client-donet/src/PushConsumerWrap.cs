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
