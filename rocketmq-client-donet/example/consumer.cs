using System;
using System.Runtime.InteropServices;
using System.Threading;
using RocketMQ.Interop;

namespace rocketmq_push_consumer_test
{
    class Program
    {
        private static readonly PushConsumerWrap.MessageCallBack callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        static void Main(string[] args)
        {
            //Task.Run(() => {
            Console.WriteLine("start push consumer...");

            var consumer = PushConsumerWrap.CreatePushConsumer("xxx");
            Console.WriteLine($"consumer: {consumer}");
            var r0 = PushConsumerWrap.SetPushConsumerLogLevel(consumer, CLogLevel.E_LOG_LEVEL_TRACE);

            var groupId = PushConsumerWrap.GetPushConsumerGroupID(consumer);
            Console.WriteLine($"groupId: {groupId}");

            var r1 = PushConsumerWrap.SetPushConsumerNameServerAddress(consumer, "47.101.55.250:9876");
            var r2 = PushConsumerWrap.Subscribe(consumer, "test", "*");
            var r3 = PushConsumerWrap.RegisterMessageCallback(consumer, callback);
            var r10 = PushConsumerWrap.StartPushConsumer(consumer);
            Console.WriteLine($"start push consumer ptr: {r10}");

            while (true)
            {
                Thread.Sleep(500);
            }
            //});
            Console.ReadKey(true);

            //PushConsumerBinder.DestroyPushConsumer(consumer);
        }

        public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
        {
            Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");

            var body = MessageWrap.GetMessageBody(message);
            var messageId = MessageWrap.GetMessageId(message);
            Console.WriteLine($"body: {body}");

            return 0;
        }
    }
}
