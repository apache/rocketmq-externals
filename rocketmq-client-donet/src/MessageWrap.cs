using System;
using System.Runtime.InteropServices;

namespace RocketMQ.Interop
{
    public static class MessageWrap
    {
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr CreateMessage(string topic);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetMessageTags(IntPtr messageIntPtr, string tags);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int SetMessageBody(IntPtr messageIntPtr, string body);
        
        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPTStr)]
        public static extern string GetMessageTopic(IntPtr message);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPTStr)]
        public static extern string GetMessageTags(IntPtr message);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPTStr)]
        public static extern string GetMessageKeys(IntPtr message);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPStr)]
        public static extern string GetMessageBody(IntPtr message);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPStr)]
        public static extern string GetMessageProperty(IntPtr message, string key);

        [DllImport(ConstValues.RocketMQDriverDllName, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPStr)]
        public static extern string GetMessageId(IntPtr message);
    }
}
