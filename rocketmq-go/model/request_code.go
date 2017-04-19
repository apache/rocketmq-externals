package rocketmq

const (
	// Broker 发送消息
	SendMsg = 10
	// Broker 订阅消息
	PullMsg = 11
	// Broker 查询消息
	QueryMESSAGE = 12
	// Broker 查询Broker Offset
	QueryBrokerOffset = 13
	// Broker 查询Consumer Offset
	QueryConsumerOffset = 14
	// Broker 更新Consumer Offset
	UpdateCconsumerOffset = 15
	// Broker 更新或者增加一个Topic
	UpdateAndCreateTopic = 17
	// Broker 获取所有Topic的配置（Slave和Namesrv都会向Master请求此配置）
	GetAllTopicConfig = 21
	// Broker 获取所有Topic配置（Slave和Namesrv都会向Master请求此配置）
	GetTopicConfigList = 22
	// Broker 获取所有Topic名称列表
	GetTopicNameList = 23
	// Broker 更新Broker上的配置
	UpdateBrokerConfig = 25
	// Broker 获取Broker上的配置
	GetBrokerConfig = 26
	// Broker 触发Broker删除文件
	TriggerDeleteFILES = 27
	// Broker 获取Broker运行时信息
	GetBrokerRuntimeInfo = 28
	// Broker 根据时间查询队列的Offset
	SearchOffsetByTimeStamp = 29
	// Broker 查询队列最大Offset
	GetMaxOffset = 30
	// Broker 查询队列最小Offset
	GetMinOffset = 31
	// Broker 查询队列最早消息对应时间
	GetEarliestMsgStoreTime = 32
	// Broker 根据消息ID来查询消息
	ViewMsgById = 33
	// Broker Client向Client发送心跳，并注册自身
	HeartBeat = 34
	// Broker Client注销
	UnregisterClient = 35
	// Broker Consumer将处理不了的消息发回服务器
	CconsumerSendMsgBack = 36
	// Broker Commit或者Rollback事务
	EndTransaction = 37
	// Broker 获取ConsumerId列表通过GroupName
	GetConsumerListByGroup = 38
	// Broker 主动向Producer回查事务状态
	CheckTransactionState = 39
	// Broker Broker通知Consumer列表变化
	NotifyConsumerIdsChanged = 40
	// Broker Consumer向Master锁定队列
	LockBatchMq = 41
	// Broker Consumer向Master解锁队列
	UNLockBatchMq = 42
	// Broker 获取所有Consumer Offset
	GetAllCconsumerOffset = 43
	// Broker 获取所有定时进度
	GetAllDelayOffset = 45
	// Namesrv 向Namesrv追加KV配置
	PutKVConfig = 100
	// Namesrv 从Namesrv获取KV配置
	GetKVConfig = 101
	// Namesrv 从Namesrv获取KV配置
	DeleteKVConfig = 102
	// Namesrv 注册一个Broker，数据都是持久化的，如果存在则覆盖配置
	RegisterBroker = 103
	// Namesrv 卸载一个Broker，数据都是持久化的
	UnregisterBroker = 104
	// Namesrv 根据Topic获取Broker Name、队列数(包含读队列与写队列)
	GetRouteinfoByTopic = 105
	// Namesrv 获取注册到Name Server的所有Broker集群信息
	GetBrokerClusterInfo             = 106
	UpdateAndCreateSubscriptionGroup = 200
	GetAllSubscriptionGroupConfig    = 201
	GetTopicStatsInfo                = 202
	GetConsumerConnList              = 203
	GetProducerConnList              = 204
	WipeWritePermOfBroker            = 205

	// 从Name Server获取完整Topic列表
	GetAllTopicListFromNamesrv = 206
	// 从Broker删除订阅组
	DeleteSubscriptionGroup = 207
	// 从Broker获取消费状态（进度）
	GetConsumeStats = 208
	// Suspend Consumer消费过程
	SuspendConsumer = 209
	// Resume Consumer消费过程
	ResumeConsumer = 210
	// 重置Consumer Offset
	ResetCconsumerOffsetInConsumer = 211
	// 重置Consumer Offset
	ResetCconsumerOffsetInBroker = 212
	// 调整Consumer线程池数量
	AdjustCconsumerThreadPoolPOOL = 213
	// 查询消息被哪些消费组消费
	WhoConsumeTHE_MESSAGE = 214

	// 从Broker删除Topic配置
	DeleteTopicInBroker = 215
	// 从Namesrv删除Topic配置
	DeleteTopicInNamesrv = 216
	// Namesrv 通过 project 获取所有的 server ip 信息
	GetKvConfigByValue = 217
	// Namesrv 删除指定 project group 下的所有 server ip 信息
	DeleteKvConfigByValue = 218
	// 通过NameSpace获取所有的KV List
	GetKvlistByNamespace = 219

	// offset 重置
	ResetCconsumerClientOffset = 220
	// 客户端订阅消息
	GetCconsumerStatusFromClient = 221
	// 通知 broker 调用 offset 重置处理
	InvokeBrokerToResetOffset = 222
	// 通知 broker 调用客户端订阅消息处理
	InvokeBrokerToGetCconsumerSTATUS = 223

	// Broker 查询topic被谁消费
	// 2014-03-21 Add By shijia
	QueryTopicConsumeByWho = 300

	// 获取指定集群下的所有 topic
	// 2014-03-26
	GetTopicsByCluster = 224

	// 向Broker注册Filter Server
	// 2014-04-06 Add By shijia
	RegisterFilterServer = 301
	// 向Filter Server注册Class
	// 2014-04-06 Add By shijia
	RegisterMsgFilterClass = 302
	// 根据 topic 和 group 获取消息的时间跨度
	QueryConsumeTimeSpan = 303
	// 获取所有系统内置 Topic 列表
	GetSysTopicListFromNS     = 304
	GetSysTopicListFromBroker = 305

	// 清理失效队列
	CleanExpiredConsumequeue = 306

	// 通过Broker查询Consumer内存数据
	// 2014-07-19 Add By shijia
	GetCconsumerRunningInfo = 307

	// 查找被修正 offset (转发组件）
	QueryCorrectionOffset = 308

	// 通过Broker直接向某个Consumer发送一条消息，并立刻消费，返回结果给broker，再返回给调用方
	// 2014-08-11 Add By shijia
	ConsumeMsgDirectly = 309

	// Broker 发送消息，优化网络数据包
	SendMsgV2 = 310

	// 单元化相关 topic
	GetUnitTopicList = 311
	// 获取含有单元化订阅组的 Topic 列表
	GetHasUnitSubTopicList = 312
	// 获取含有单元化订阅组的非单元化 Topic 列表
	GetHasUnitSubUnunitTopicList = 313
	// 克隆某一个组的消费进度到新的组
	CloneGroupOffset = 314

	// 查看Broker上的各种统计信息
	ViewBrokerStatsData = 315
)
