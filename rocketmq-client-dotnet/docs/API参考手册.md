#  .NET API 参考
  
- [.NET API 参考](#net-api-参考 )
  - [MQProducer API](#mqproducer-api )
    - [(构造函数)](#构造函数 )
    - [开启和关闭Producer](#开启和关闭producer )
      - [StartProducer](#startproducer )
      - [ShutdownProducer](#shutdownproducer )
      - [DestroyProducer](#destroyproducer )
    - [设置相关参数](#设置相关参数 )
      - [SetProducerNameServerAddress](#setproducernameserveraddress )
      - [SetProducerNameServerDomain](#setproducernameserverdomain )
      - [SetProducerGroupName](#setproducergroupname )
      - [SetProducerInstanceName](#setproducerinstancename )
      - [SetProducerSessionCredentials](#setproducersessioncredentials )
      - [SetProducerLogPath](#setproducerlogpath )
      - [SetProducerLogFileNumAndSize](#setproducerlogfilenumandsize )
      - [SetProducerLogLevel](#setproducerloglevel )
      - [SetAutoRetryTimes](#setautoretrytimes )
    - [发送消息API](#发送消息api )
      - [SendMessageSync](#sendmessagesync )
      - [SendMessageOneway](#sendmessageoneway )
      - [SendMessageOrderly](#sendmessageorderly )
  - [MQPushConsumer](#mqpushconsumer )
    - [（构造函数）](#构造函数-1 )
    - [开启和关闭PushConsumer](#开启和关闭pushconsumer )
      - [StarPushConsumer](#starpushconsumer )
      - [ShutdownPushConsumer](#shutdownpushconsumer )
      - [DestroyPushConsumer](#destroypushconsumer )
    - [设置参数API](#设置参数api )
      - [SetPushConsumerGroupId](#setpushconsumergroupid )
      - [SetPushConsumerNameServerAddress](#setpushconsumernameserveraddress )
      - [SetPushConsumerNameServerDomain](#setpushconsumernameserverdomain )
      - [SetPushConsumerThreadCount](#setpushconsumerthreadcount )
      - [SetPushConsumerMessageBatchMaxSize](#setpushconsumermessagebatchmaxsize )
      - [SetPushConsumerInstanceName](#setpushconsumerinstancename )
      - [SetPushConsumerSessionCredentials](#setpushconsumersessioncredentials )
      - [SetPushConsumerLogPath](#setpushconsumerlogpath )
      - [SetPushConsumerLogLevel](#setpushconsumerloglevel )
      - [SetPushConsumerLogLevel](#setpushconsumerloglevel-1 )
      - [SetPushConsumerMessageModel](#setpushconsumermessagemodel )
    - [Push Message API](#push-message-api )
      - [Subscribe](#subscribe )
      - [RegisterMessageCallback](#registermessagecallback )
    - [other](#other )
      - [GetPushConsumerGroupID](#getpushconsumergroupid )
  - [MQPullConsumer](#mqpullconsumer )
    - [（构造函数）](#构造函数-2 )
    - [开启和关闭PullConsumer](#开启和关闭pullconsumer )
      - [StarPullConsumer](#starpullconsumer )
      - [ShutdownPullConsumer](#shutdownpullconsumer )
      - [DestroyPullConsumer](#destroypullconsumer )
    - [设置参数API](#设置参数api-1 )
      - [SetPullConsumerGroupId](#setpullconsumergroupid )
      - [SetPullConsumerNameServerAddress](#setpullconsumernameserveraddress )
      - [SetPullConsumerNameServerDomain](#setpullconsumernameserverdomain )
      - [SetPullConsumerThreadCount](#setpullconsumerthreadcount )
      - [SetPullConsumerMessageBatchMaxSize](#setpullconsumermessagebatchmaxsize )
      - [SetPullConsumerInstanceName](#setpullconsumerinstancename )
      - [SetPullConsumerSessionCredentials](#setpullconsumersessioncredentials )
      - [SetPullConsumerLogPath](#setpullconsumerlogpath )
      - [SetPullConsumerLogLevel](#setpullconsumerloglevel )
      - [SetPullConsumerLogLevel](#setpullconsumerloglevel-1 )
    - [Pull Message API](#pull-message-api )
      - [FetchSubscriptionMessageQueues](#fetchsubscriptionmessagequeues )
      - [Pull](#pull )
      - [getMessageQueueOffset](#getmessagequeueoffset )
  - [Message](#message )
    - [（构造函数）](#构造函数-3 )
    - [设置参数对应API](#设置参数对应api )
      - [SetMessageTopic](#setmessagetopic )
      - [SetMessageTags](#setmessagetags )
      - [SetMessageKeys](#setmessagekeys )
      - [SetMessageBody](#setmessagebody )
      - [SetMessageProperty](#setmessageproperty )
  - [其他相关数据结构](#其他相关数据结构 )
    - [SendResult](#sendresult )
    - [CPullStatus](#cpullstatus )
    - [MessageModel](#messagemodel )
    - [MessageQueue](#messagequeue )
    - [CMessageQueue](#cmessagequeue )
  
##   MQProducer API
  
  
###  (构造函数) 
  
  
*  **Functions**
  
```C#
 MQProducer(string groupName, DiagnosticListener diagnosticListener = null);
  
 MQProducer(string groupName,string nameServerAddress, DiagnosticListener diagnosticListener = null);
  
 MQProducer(string groupName, string nameServerAddress,string logPath, DiagnosticListener diagnosticListener = null)
  
 MQProducer(string groupName, string nameServerAddress, string logPath,LogLevel logLevel, DiagnosticListener diagnosticListener = null)
  
  
```
  
*  **Consruct MQProducer**
  
构造一个MQProducer对象
  
* **Parameters**
  
`groupName`  生产者对应的组
  
`nameServerAddress`  nameServer 地址
  
`logPath`  日志路径
  
`logLevel` 日志级别 
  
###  开启和关闭Producer
  
  
####  StartProducer
  
  
```C#
bool StartProducer()
```
  
* **StartProducer**
  
    开启生产者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  ShutdownProducer
  
  
```C#
bool ShutdownProducer()
```
  
* **ShutdownProducer**
  
    关闭生产者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  DestroyProducer
  
  
```C#
bool DestroyProducer()
```
  
* **DestoryProducer**
  
    注销生产者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
  
  
###  设置相关参数
  
  
####  SetProducerNameServerAddress
  
  
```C#
 void SetProducerNameServerAddress(string nameServerAddress)
```
*  **SetNameServerAddress**
  
    设置`Producer`的地址
* **Parameters**
  
    `nameServerAddress` nameServer 地址
  
  
  
####  SetProducerNameServerDomain
  
  
```C#
 void SetProducerNameServerDomain(string nameServerDomain)
```
  
* **SetProducerNameServerDomain**
  
    使用Domain设置`Producer`的地址
* **Parameters**
  
`nameServerDomain` nameServer Domain
  
####  SetProducerGroupName
  
  
  
```C#
 void SetProducerGroupName(string groupName)
```
  
* **SetProducerGroupName**
    设置生产者的组号
* **Parameters**
  
`groupName`
    生产者对应的组
  
  
  
####  SetProducerInstanceName
  
  
```C#
 void SetProducerInstanceName(string instanceName)
```
  
* **SetProducerInstanceName**
   设置生产者实例名称
* **Parameters**
  
`instanceName`
   生产者的实例名称
  
  
####  SetProducerSessionCredentials
  
  
```C#
void SetProducerSessionCredentials(string accessKey, string secretKey, string onsChannel)
```
  
* **SetProducerSessionCredentials**
   设置生产者的认证信息，链接阿里云rocektmq时必要设置参数
* **Parameters**
  
    `accessKey`
    阿里云账户 `accessKey`
  
    `secretKey`
    阿里云账户 `secretKey`
  
    `onsChannel`
    一般默认值为 `ALIYUN`
  
####  SetProducerLogPath
  
  
```C#
void SetProducerLogPath(string logPath)
```
  
* **SetProducerLogPaths**
   设置生产者的日志路径
* **Parameters**
  
    `logPath`
    日志路径
  
####   SetProducerLogFileNumAndSize
  
  
```C#
void SetProducerLogFileNumAndSize(int fileNum, long fileSize)
```
  
* **SetProducerLogFileNumAndSize**
   设置生产者的日志文件数量和大小
* **Parameters**
`fileNum`
   日志文件数量
`fileSize`
   日志文件大小
  
####  SetProducerLogLevel
  
  
```C#
void SetProducerLogLevel(LogLevel logLevel)
```
  
* **SetProducerLogFileNumAndSize**
   设置生产者的日志文件数量和大小
* **Parameters**
`logLevel`
  枚举日志级别（详见LogLeveL）
  
####  SetAutoRetryTimes
  
  
```C#
void SetAutoRetryTimes(int times)
```
  
* **SetAutoRetryTimes**
   设置生产者发送消息失败时，重试次数
* **Parameters**
`times`
  生产者发送消息失败时，重试次数
  
###  发送消息API
  
  
####  SendMessageSync
  
  
```C#
SendResult SendMessageSync(HandleRef message)
  
```
* **SendMessageSync**
   同步消息发送
* **Parameters**
`message`
  消息handleRef
  
* **return**
    SendResult 对象（详见SendResult）
  
####  SendMessageOneway
  
  
```C#
SendResult SendMessageOneway(HandleRef message)
  
```
* **SendMessageOneway**
   单向消息发送
* **Parameters**
`message`
  消息handleRef
  
* **return**
    SendResult 对象（详见SendResult）
  
####  SendMessageOrderly
  
  
```C#
SendResult SendMessageOrderly(HandleRef message, QueueSelectorCallback callback, string args = "")
  
```
* **SendMessageOrderly**
   顺序消息发送
* **Parameters**
`message`
  消息handleRef
`callback`
  回调函数
`args`
  参数
* **return**
    SendResult 对象（详见SendResult）
  
##  MQPushConsumer
  
  
###  （构造函数）
  
*  **Functions**
  
```C#
MQPushConsumer(string groupId)
  
MQPushConsumer(string groupId, string nameServerAddress)
  
MQPushConsumer(string groupId, string nameServerAddress, string logPath, LogLevel logLevel)
  
```
  
*  **Consruct MQPushConsumer**
  
构造一个MQProducer对象
  
* **Parameters**
`groupName`
  
    消费者对应的组
`nameServerAddress`
    nameServer 地址
`logPath` 
  
    日志路径
`logLevel`
  
   枚举值，日志级别 
  
###  开启和关闭PushConsumer
  
  
####  StarPushConsumer
  
  
```C#
bool StartPushConsumer()
```
  
* **StartPushConsumer**
  
    开启消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  ShutdownPushConsumer
  
  
```C#
bool ShutdownPushConsumer()
```
  
* **ShutdownPushConsumer**
  
    关闭消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  DestroyPushConsumer
  
  
```C#
bool DestroyPushConsumer()
```
  
* **DestoryPushConsumer**
  
    注销消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
###  设置参数API
  
  
  
####  SetPushConsumerGroupId
  
  
```C#
void SetPushConsumerGroupId(string groupId)
```
* **SetPushConsumerGroupId**
   设置PushConsumer的组
* **Parameters**
`groupId`
    组号
  
  
####  SetPushConsumerNameServerAddress
  
  
```C#
void SetPushConsumerNameServerAddress(string nameServerAddress)
```
* **SetPushConsumerNameServerAddress**
   设置PushConsumer的组
* **Parameters**
`nameServerAddress`
nameServer地址信息
  
####  SetPushConsumerNameServerDomain
  
  
```C#
void SetPushConsumerNameServerDomain(string domain)
```
* **SetPushConsumerNameServerDomain**
   设置PushConsumer的NameServer Domain
* **Parameters**
`nameServerAddress`
nameServer地址信息
  
####  SetPushConsumerThreadCount
  
  
```C#
void SetPushConsumerThreadCount(int threadCount)
```
* **SetPushConsumerThreadCount**
   设置PushConsumer的线程数
* **Parameters**
`threadCount`
线程数
  
####  SetPushConsumerMessageBatchMaxSize
  
  
```C#
void SetPushConsumerMessageBatchMaxSize(int batchSize)
```
* **SetPushConsumerMessageBatchMaxSize(**
   设置PushConsumer的消息的batch最大值
* **Parameters**
`batchSize`
大小
  
####  SetPushConsumerInstanceName
  
  
```C#
void SetPushConsumerInstanceName(string instanceName)
```
* **SetPushConsumerInstanceName**
   设置PushConsumer的实例名称
* **Parameters**
`instanceName`
实例名称
  
####  SetPushConsumerSessionCredentials
  
  
```C#
void SetPushConsumerSessionCredentials(string accessKey,string secretKey,string channel)
```
* **SetPushConsumerSessionCredentials**
   设置PushConsume的认证信息，使用rocketmq时必须填写
* **Parameters**
`accessKey`
阿里云accessKey
`secretKey`
阿里云secretKey
`channel`
一般默认值为`ALIYUN`
  
####  SetPushConsumerLogPath
  
  
```C#
void SetPushConsumerLogPath(string logPath)
```
* **SetPushConsumerLogPath**
   设置PushConsume的日志路径
* **Parameters**
`logPath`
 PushConsume 的日志路径，默认值为运行环境
  
####  SetPushConsumerLogLevel
  
  
```C#
void SetPushConsumerLogLevel(string logLevel)
```
* **SetPushConsumerLogLevel**
   设置PushConsume的日志级别
* **Parameters**
`logLevel`
 PushConsume 的日志级别
  
####  SetPushConsumerLogLevel
  
  
```C#
void SetPushConsumerLogFileNumAndSize(int fileNum, long fileSize)
```
* **SetPushConsumerLogFileNumAndSize**
   设置PushConsume的日志文件数量和大小
* **Parameters**
`fileNum`
 PushConsume 的日志文件数量
 `fileSize`
文件大小
  
####  SetPushConsumerMessageModel
  
  
```C#
void SetPushConsumerMessageModel(MessageModel messageModel)
```
* **SetPushConsumerMessageModel**
   设置PushConsume的消息类型
* **Parameters**
`messageModel`
枚举值，PushConsume 的消息模式 分别为 `Broadcasting`和`Clustering`两种类型
  
  
###  Push Message API
  
  
####  Subscribe
  
  
```C#
void Subscribe(string topic, string expression)
```
* **Subscribe**
   设置PushConsume的消息类型
* **Parameters**
`topic`  
topic 名称
`expression` 
消息过滤时可以使用表达式进行设置，不需过滤时使用 `"*"`
  
####  RegisterMessageCallback
  
  
```C#
bool RegisterMessageCallback(PushConsumerWrap.MessageCallBack callBack)
```
  
* **RegisterMessageCallback**
   注册回调函数
* **Parameters**
`callBack`
委托函数 `PushConsumerWrap.MessageCallBack `的一个实例函数
```C#
// 委托函数声明
 public delegate int MessageCallBack(IntPtr consumer, IntPtr messageIntPtr); 
```
* 示例
  
```C#
//函数实现
public static int HandleMessageCallBack(IntPtr consumer, IntPtr message)
{
     Console.WriteLine($"consumer: {consumer}; messagePtr: {message}");
  
    var body = MessageWrap.GetMessageBody(message);
    Console.WriteLine($"body: {body}");
  
    var messageId = MessageWrap.GetMessageId(message);
    Console.WriteLine($"message_id: {messageId}");
  
    return 0;
}
  
//注册函数
private static readonly PushConsumerWrap.MessageCallBack _callback = new PushConsumerWrap.MessageCallBack(HandleMessageCallBack);
  
//注册PushConsumer的回调函数
consumer.RegisterMessageCallback(_callback);
  
```
###  other
  
  
  
####  GetPushConsumerGroupID
  
  
```C#
string GetPushConsumerGroupID() 
```
  
* **GetPushConsumerGroupID**
  
    获取消费者的组号
* **Parameters**
  
    (null)
* **Return**
`string`类型的PushConsumer的组号 
  
  
##  MQPullConsumer
  
  
###  （构造函数）
  
*  **Functions**
  
```C#
MQPullConsumer(string groupId)
  
MQPullConsumer(string groupId, string nameServerAddress)
  
MQPullConsumer(string groupId, string nameServerAddress, string logPath, LogLevel logLevel)
  
```
  
*  **Consruct MQPullConsumer**
  
构造一个MQProducer对象
  
* **Parameters**
`groupName`
  
    消费者对应的组
`nameServerAddress`
    nameServer 地址
`logPath` 
  
    日志路径
`logLevel`
  
   枚举值，日志级别 
  
###  开启和关闭PullConsumer
  
  
####  StarPullConsumer
  
  
```C#
bool StartPullConsumer()
```
  
* **StartPullConsumer**
  
    开启消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  ShutdownPullConsumer
  
  
```C#
bool ShutdownPullConsumer()
```
  
* **ShutdownPullConsumer**
  
    关闭消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
####  DestroyPullConsumer
  
  
```C#
bool DestroyPullConsumer()
```
  
* **DestoryPullConsumer**
  
    注销消费者
* **Parameters**
  
    (null)
* **Return**
  
  *success: true*
  
  *fail: false*
  
###  设置参数API
  
  
  
####  SetPullConsumerGroupId
  
  
```C#
void SetPullConsumerGroupId(string groupId)
```
* **SetPullConsumerGroupId**
   设置PullConsumer的组
* **Parameters**
`groupId`
    组号
  
  
####  SetPullConsumerNameServerAddress
  
  
```C#
void SetPullConsumerNameServerAddress(string nameServerAddress)
```
* **SetPullConsumerNameServerAddress**
   设置PullConsumer的组
* **Parameters**
`nameServerAddress`
nameServer地址信息
  
####  SetPullConsumerNameServerDomain
  
  
```C#
void SetPullConsumerNameServerDomain(string domain)
```
* **SetPullConsumerNameServerDomain**
   设置PullConsumer的NameServer Domain
* **Parameters**
`nameServerAddress`
nameServer地址信息
  
####  SetPullConsumerThreadCount
  
  
```C#
void SetPullConsumerThreadCount(int threadCount)
```
* **SetPullConsumerThreadCount**
   设置PullConsumer的线程数
* **Parameters**
`threadCount`
线程数
  
####  SetPullConsumerMessageBatchMaxSize
  
  
```C#
void SetPullConsumerMessageBatchMaxSize(int batchSize)
```
* **SetPullConsumerMessageBatchMaxSize(**
   设置PullConsumer的消息的batch最大值
* **Parameters**
`batchSize`
大小
  
####  SetPullConsumerInstanceName
  
  
```C#
void SetPullConsumerInstanceName(string instanceName)
```
* **SetPullConsumerInstanceName**
   设置PullConsumer的实例名称
* **Parameters**
`instanceName`
实例名称
  
####  SetPullConsumerSessionCredentials
  
  
```C#
void SetPullConsumerSessionCredentials(string accessKey,string secretKey,string channel)
```
* **SetPullConsumerSessionCredentials**
   设置PullConsume的认证信息，使用rocketmq时必须填写
* **Parameters**
`accessKey`
阿里云accessKey
`secretKey`
阿里云secretKey
`channel`
一般默认值为`ALIYUN`
  
####  SetPullConsumerLogPath
  
  
```C#
void SetPullConsumerLogPath(string logPath)
```
* **SetPullConsumerLogPath**
   设置PullConsume的日志路径
* **Parameters**
`logPath`
 PullConsume 的日志路径，默认值为运行环境
  
####  SetPullConsumerLogLevel
  
  
```C#
void SetPullConsumerLogLevel(string logLevel)
```
* **SetPullConsumerLogLevel**
   设置PullConsume的日志级别
* **Parameters**
`logLevel`
 PullConsume 的日志级别
  
####  SetPullConsumerLogLevel
  
  
```C#
void SetPullConsumerLogFileNumAndSize(int fileNum, long fileSize)
```
* **SetPullConsumerLogFileNumAndSize**
   设置PullConsume的日志文件数量和大小
* **Parameters**
`fileNum`
 PullConsume 的日志文件数量
 `fileSize`
文件大小
  
###  Pull Message API
  
  
####  FetchSubscriptionMessageQueues
  
  
```C#
CMessageQueue[] FetchSubscriptionMessageQueues(string topic)
```
* **SFetchSubscriptionMessageQueues**
   Fetch 消息队列
* **Parameters**
`topic`
 topic 的名称
  
* **Return**
`CMessageQueue`的数组，类型查看`CMessageQueue`
  
####  Pull
  
  
```C#
CPullResult Pull(MessageQueue mq,CMessageQueue msg, string subExpression, long offset, int maxNums)
```
* **Pull**
   Fetch 消息队列
* **Parameters**
`mq`
 消息队列mq
`msg`
 `CMessageQueue`的消息队列
`subExpression`
    子表达式
`offset` 
    队列offset
`maxNums`
    最大的数量
* **Return**
`CPullResult`类型的结果
  
  
####  getMessageQueueOffset
  
  
```C#
 static long getMessageQueueOffset(MessageQueue mq)
  
```
  
* **getMessageQueueOffset**
  
   获取到队列对应的下一个offset
* **Parameters**
`mq`
 消息队列mq
  
* **Return**
`mq`的下一个`offset`
  
##  Message
  
  
###  （构造函数）
  
  
```C#
  
MQMessage(string topic);
  
MQMessage(string topic,string messageBody,string messageTags);
  
```
*  **Consruct MQMessage**
  
构造一个MQMessage对象
  
* **Parameters**
  
`topic`  消息topic
  
`messageBody`  消息主体
  
`messageTags`  消息tag
  
###  设置参数对应API
  
  
####  SetMessageTopic
  
  
```C#
void SetMessageTopic(string topic)
```
* **SetMessageTopic**
   设置消息Topic
* **Parameters**
`topic`
 消息topic
  
  
####  SetMessageTags
  
  
```C#
void SetMessageTags(string tags)
```
* **SetMessageTags**
   设置消息Tags
* **Parameters**
`tags`
 消息tag
  
  
####  SetMessageKeys
  
  
```C#
void SetMessageKeys(string keys)
```
* **SetMessageKeys**
   设置消息key
* **Parameters**
`tags`
 消息key
  
####  SetMessageBody
  
  
```C#
void SetMessageBody(string body)
```
* **SetMessageBody**
   设置消息body
* **Parameters**
`body`
 消息body
  
####  SetMessageProperty
  
  
```C#
void SetMessageProperty(string key, string value)
```
* **SetMessageProperty**
   设置消息值
* **Parameters**
`key`
属性key
`value`
属性value
  
##  其他相关数据结构
  
  
###  SendResult
  
  
```C#
public class SendResult
{
    public int SendStatus { get; set; } //发送状态
  
    public string MessageId { get; set; }// 消息ID
  
    public long Offset { get; set; }// offset
}
  
```
  
###  CPullStatus
  
拉取消息状态
  
```C#
public enum CPullStatus
{
    E_FOUND = 1, // 发现
    E_NO_NEW_MSG =2,// 没有新消息
    E_NO_MATCHED_MSG =3,// 没有匹配的消息
    E_OFFSET_ILLEGAL = 4, // 不合法的OFFSET
    E_BROKER_TIMEOUT = 5 // 时间超时
}
  
```
  
###  MessageModel
  
  
消息消费模式
  
```C#
  
public enum MessageModel
{
    Broadcasting = 0,//广播消费模式
  
    Clustering = 1 // 集群消费模式
}
```
  
###   MessageQueue
  
  
消息队列类型
  
```C#
public class MessageQueue
{
    public string topic { get; set; }
  
    public string brokeName { get; set; }
  
    public int queueId { get; set; }
}
```
  
###  CMessageQueue
  
  
集成自`C`的数据结构
```C#
  
[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
public struct CMessageQueue
{
    [MarshalAs(UnmanagedType.ByValArray, SizeConst = 512)]
    public char[] topic;
    [MarshalAs(UnmanagedType.ByValArray, SizeConst = 256)]
    public char[] brokerName;
    public int queueId;
};
  
```
  
  
  