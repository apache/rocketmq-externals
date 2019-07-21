using RocketMQ.NetClient.Consumer;
using RocketMQ.NetClient.Interop;
using RocketMQ.NetClient.Message;
using RocketMQ.NETClient.Consumer;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace PullConsumerQuickStart
{
    class Program
    {
       
        
        static void Main(string[] args)
        {
            
            Console.Title = "PushConsumer";

            Task.Run(() => {
                Console.WriteLine("start Pull consumer...");

                //创建一个PullConsumer
                MQPullConsumer consumer = new MQPullConsumer("xxx", "127.0.0.1:9876", ".\\log.txt", LogLevel.Trace);

                //开启消费者
                var result = consumer.StartPullConsumer();
                Console.WriteLine($"start Pull consumer ? : {result}");

                //填充消息队列
                CMessageQueue[] msgs = consumer.FetchSubscriptionMessageQueues("test");
                

                for (int j = 0; j < msgs.Length; j++)
                {
                    int flag = 0;
                   

                    Console.WriteLine("msg topic : " + new string(msgs[j].topic));

                    MessageQueue mq = new MessageQueue { topic = new string(msgs[j].topic), brokeName = new string(msgs[j].brokerName), queueId = msgs[j].queueId };
                    
                    while (true)
                    {
                    try
                    {
                        
                        //主动拉取消费
                        CPullResult cPullResult = consumer.Pull(mq,msgs[j], "", MQPullConsumer.getMessageQueueOffset(mq), 32);
                        Console.WriteLine(new string(msgs[j].topic) + " status : " + cPullResult.pullStatus +"Max offset "+ cPullResult.maxOffset + " offset: " + cPullResult.nextBeginOffset + " Quene Id" + msgs[j].queueId);
                        //Console.WriteLine(" " + msg.topic);
                        long a = cPullResult.nextBeginOffset;
                        MQPullConsumer.putMessageQueueOffset(mq, a);
                        switch (cPullResult.pullStatus)
                        {
                            case CPullStatus.E_FOUND:
                                break;
                            case CPullStatus.E_NO_MATCHED_MSG:
                                break;
                            case CPullStatus.E_NO_NEW_MSG:
                                flag = 1;
                                break;
                            case CPullStatus.E_OFFSET_ILLEGAL:
                                flag = 2;
                                break;
                            default:
                                break;
                        }

                        if(flag == 1|| cPullResult.nextBeginOffset == cPullResult.maxOffset) { break; }
                        if (flag == 2) {
                               // Console.WriteLine("OFFSET_ILLEGAL");
                                break; }

                        }
                        catch (Exception e)
                        {
                            Console.WriteLine(e.Message);
                        }
                    }

                  
                }
                

                while (true)
                {
                    Thread.Sleep(500);
                }
            });
            Console.ReadKey(true);

            //PullConsumerBinder.DestroyPullConsumer(consumer);
        }
       

    }
}
