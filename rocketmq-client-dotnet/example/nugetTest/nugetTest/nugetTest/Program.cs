using System;
using System.Threading;
using RocketMQ.NetClient.Message;
using RocketMQ.NetClient.Producer;
namespace nugetTest
{
    class Program
    {
        private static ProducerWrap.QueueSelectorCallback _queueSelectorCallback = new ProducerWrap.QueueSelectorCallback(
           (size, message, args) =>
           {
               Console.WriteLine($"size: {size}, message: {message}, ptr: {args}");

               return 0;
           });

        static void Main(string[] args)
        {
            Console.Title = "Producer";

            Console.WriteLine("Start create producer.");

            MQProducer producer = new MQProducer("GroupA", "127.0.0.1:9876");
            producer.StartProducer();

            try
            {
                while (true)
                {
                    // message
                    MQMessage message = new MQMessage("test");

                    // SendMessageSync
                    //var sendResult = producer.SendMessageSync(message);
                    //Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResult.MessageId);

                    // SendMessageOneway
                    //var sendResult = producer.SendMessageOneway(message);
                    //Console.WriteLine("send result:" + sendResult);

                    // SendMessageOneWay
                    var sendResult = producer.SendMessageOrderly(message.GetHandleRef(), _queueSelectorCallback, "aa");
                    Console.WriteLine("send result:" + sendResult.MessageId);

                    Thread.Sleep(500);
                }
                var shutdownResult = producer.ShutdownProducer();
                Console.WriteLine("shutdown result:" + shutdownResult);

                var destoryResult = producer.DestroyProducer();
                Console.WriteLine("destory result:" + destoryResult);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.ToString());
            }
            Console.ReadKey(true);
        }
    }
}
