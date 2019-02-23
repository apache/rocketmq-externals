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
