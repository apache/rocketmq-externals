# .NET 客户端QuickStart
[toc]

## 环境准备

### 1. 开发工具

**Visual Studio 2015** or **Visual Studio 2017**



### 2. 本机测试环境

如果您使用阿里云rocketmq，或者熟悉RocketMQ环境，您可以跳过此步骤。

* 参考文档

    [本机测试环境](.\rocketmq本机测试环境搭建.md)

## 本地QuickStart

### Producer



#### 1. 控制台应用 （.NET Core）

* 环境要求

```
    .NETStandard >=v2.0
    x64运行环境
```

* 步骤总览
  
```
    1. 创建控制台应用（Core）   
    2. 引入`rocketmq-client-dotnet` **Nuget**包
    3. 编写Producer代码
    4. 运行测试`
```
* 创建控制台应用（.NET Core)）


1. 使用visual Studio 创建新项目，选择**c#** `控制台应用(.NET Core)`

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core1.png)


2. 右键点击项目 => `管理NuGet程序包`

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core2.png)

3.  进入`NuGet管理界面`，如下图
   
     
   * 输入rocketmq-client-dotnet
   * 查看对应包信息
   *  点击安装

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core3.png)
 

4. 编写测试程序，在 `Programs.cs`中添加代码如下

```C#

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
           
            //创建一个 producer
            MQProducer producer = new MQProducer("GroupA", "127.0.0.1:9876");
            producer.StartProducer();


            try
            {
                while (true)
                {
                    // 创建一个消息 message
                    MQMessage message = new MQMessage("NETMessage"); 

                    // 使用producer发送消息
                    // SendMessageSync
                    var sendResult = producer.SendMessageSync(message.GetHandleRef());
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResult.MessageId);
   

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

```

5. 以x64启动调试该程序，可以看到下图

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/FR4.png)

#### 2. 控制台应用（.NET Framework ）

* 环境要求

```
    .NET FrameWork >=4.6.1
    x64运行环境
```

* 步骤总览
  
```
    1. 创建控制台应用（.NET Framework）
    2. 引入`rocketmq-client-dotnet` **Nuget**包
    3. 编写Producer代码
    4. 运行测试
```


1. 使用visual Studio 创建新项目，选择**c#** `控制台应用(.NET FrameWork)` 

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/FR1.png)

2. 右键点击项目 => `管理NuGet程序包`
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core2.png)
3.  进入`NuGet管理界面`，如下图
   
   * 输入rocketmq-client-dotnet
   * 查看对应包信息
   *  点击安装

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core3.png)

4. 看到项目文件多一个`CClient`文件夹下`rocketmq-client-dll`,对其属性进行更改操作

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/FR2.png)

5. 编写测试程序，在 `Programs.cs`中添加代码如下

```C#

using RocketMQ.NetClient.Message;
using RocketMQ.NetClient.Producer;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ProducerFrameWorkTest
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
           
            //创建一个 producer
            MQProducer producer = new MQProducer("GroupA", "127.0.0.1:9876");
            producer.StartProducer();


            try
            {
                while (true)
                {
                    // 创建一个消息 message
                    MQMessage message = new MQMessage("NETMessage"); 

                    // 使用producer发送消息
                    // SendMessageSync
                    var sendResult = producer.SendMessageSync(message.GetHandleRef());
                    Console.WriteLine("send result:" + sendResult + ", msgId: " + sendResult.MessageId);
   

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

```

6. 以x64启动调试该程序，可以看到下图
   
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/FR3.png)
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/FR4.png)
### Consumer



#### 1. 控制台应用 （.NET Core）

* 环境要求

```
    .NETStandard >=v2.0
    x64运行环境
```

* 步骤总览
  
```
    1. 创建控制台应用（Core）   
    2. 引入`rocketmq-client-dotnet` **Nuget**包
    3. 编写Producer代码
    4. 运行测试`
```
* 创建控制台应用（.NET Core)）


1. 使用visual Studio 创建新项目，选择**c#** `控制台应用(.NET Core)` 
   
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCCore1.png)

2. 右键点击项目 => `管理NuGet程序包`

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCCore2.png)

3.  进入`NuGet管理界面`，如下图
   
   * 输入rocketmq-client-dotnet
   * 查看对应包信息
   *  点击安装
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core3.png)

4. 编写测试程序，在 `Programs.cs`中添加代码如下

```C#

using RocketMQ.NetClient.Consumer;
using RocketMQ.NetClient.Interop;
using RocketMQ.NetClient.Message;
using RocketMQ.NETClient.Consumer;
using System;
using System.Threading;
using System.Threading.Tasks;

namespace PushConsumerDemo
{
    class Program
    {
        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        static void Main(string[] args)
        {
            Console.Title = "PushConsumer";
            Console.WriteLine("start push consumer...");

            // 创建一个Push消费者


            Task.Run(() => {

                var consumer = new MQPushConsumer("GID_NET", "127.0.0.1:9876");
                Console.WriteLine($"consumer: {consumer}");
                // 设置日志目录和级别
                consumer.SetPushConsumerLogPath(".\\consumer_log.txt");
                consumer.SetPushConsumerLogLevel(LogLevel.Trace);

                // 获取消费者组号
                var groupId = consumer.GetPushConsumerGroupID();
                Console.WriteLine($"groupId: {groupId}");

                //  订阅一个`topic`
                consumer.Subscribe("MQ_INST_1547778772487337_Ba4IiUHE%NETP", "*");

                //注册回调函数
                consumer.RegisterMessageCallback(_callback);

                //启动消费者
                var result = consumer.StartPushConsumer();
                Console.WriteLine($"start push consumer ptr: {result}");

                while (true)
                {
                    Thread.Sleep(1000);
                }
            });
            Console.ReadKey(true);
        }

        public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
        {
            Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");

            var body = MessageWrap.GetMessageBody(message);
            Console.WriteLine($"body: {body}");

            var messageId = MessageWrap.GetMessageId(message);
            Console.WriteLine($"message_id: {messageId}");

            return 0;
        }
    }
}


```

5. 以x64启动调试该程序，可以看到下图

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCCore3.png)

#### 2. 控制台应用（.NET Framework ）

* 环境要求

```
    .NET FrameWork >=4.6.1
    x64运行环境
```

* 步骤总览
  
```
    1. 创建控制台应用（.NET Framework）
    2. 引入`rocketmq-client-dotnet` **Nuget**包
    3. 编写Producer代码
    4. 运行测试
```


1. 使用visual Studio 创建新项目，选择**c#** `控制台应用(.NET FrameWork)` 
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCFR1.png)

2. 右键点击项目 => `管理NuGet程序包`
![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCFR2.png)
3.  进入`NuGet管理界面`，如下图
   
   * 输入rocketmq-client-dotnet
   * 查看对应包信息
   *  点击安装

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/core3.png)

4. 看到项目文件多一个`CClient`文件夹下`rocketmq-client-dll`,对其属性进行更改操作

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCFR2.png)

5. 编写测试程序，在 `Programs.cs`中添加代码如下

```C#

using RocketMQ.NetClient.Consumer;
using RocketMQ.NetClient.Interop;
using RocketMQ.NetClient.Message;
using RocketMQ.NETClient.Consumer;
using System;
using System.Threading;
using System.Threading.Tasks;

namespace PushConsumerDemo
{
    class Program
    {
        private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);

        static void Main(string[] args)
        {
            Console.Title = "PushConsumer";
            Console.WriteLine("start push consumer...");

            // 创建一个Push消费者


            Task.Run(() => {

                var consumer = new MQPushConsumer("GID_NET", "127.0.0.1:9876");
                Console.WriteLine($"consumer: {consumer}");
                // 设置日志目录和级别
                consumer.SetPushConsumerLogPath(".\\consumer_log.txt");
                consumer.SetPushConsumerLogLevel(LogLevel.Trace);

                // 获取消费者组号
                var groupId = consumer.GetPushConsumerGroupID();
                Console.WriteLine($"groupId: {groupId}");

                //  订阅一个`topic`
                consumer.Subscribe("MQ_INST_1547778772487337_Ba4IiUHE%NETP", "*");

                //注册回调函数
                consumer.RegisterMessageCallback(_callback);

                //启动消费者
                var result = consumer.StartPushConsumer();
                Console.WriteLine($"start push consumer ptr: {result}");

                while (true)
                {
                    Thread.Sleep(1000);
                }
            });
            Console.ReadKey(true);
        }

        public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
        {
            Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");

            var body = MessageWrap.GetMessageBody(message);
            Console.WriteLine($"body: {body}");

            var messageId = MessageWrap.GetMessageId(message);
            Console.WriteLine($"message_id: {messageId}");

            return 0;
        }
    }
}


```

6. 以x64启动调试该程序，可以看到下图

![image]( https://raw.githubusercontent.com/ClementIV/picture-rocketmq/master/dotnet/PCCore3.png)