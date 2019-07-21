using RocketMQ.NetClient.Message;
using RocketMQ.NetClient.Producer;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ProducerDemo
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

            MQProducer producer = new MQProducer("GID_NET");
            producer.SetProducerNameServerAddress("127.0.0.1");
            producer.StartProducer();

       

            try
            {
                while (true)
                {
                    // message
                    MQMessage message = new MQMessage("MQ_INST_1547778772487337_Ba4IiUHE%NETP"); 

                    // SendMessageSync
                    var sendResult = producer.SendMessageSync(message.GetHandleRef());
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResult.MessageId);

                    // SendMessageOneway
                    //var sendResult = producer.SendMessageOneway(message);
                    //Console.WriteLine("send result:" + sendResult);

                    // SendMessageOneWay
                    //var sendResult = producer.SendMessageOrderly(message.GetHandleRef(), _queueSelectorCallback, "aa");
                    //Console.WriteLine("send result:" + sendResult.MessageId);

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
