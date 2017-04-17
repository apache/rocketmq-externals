/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef __MQPROTOS_H__
#define __MQPROTOS_H__

namespace rmq
{
    enum MQRequestCode
    {
        // Broker 发送消息
        SEND_MESSAGE_VALUE = 10,
        // Broker 订阅消息
        PULL_MESSAGE_VALUE = 11,
        // Broker 查询消息
        QUERY_MESSAGE_VALUE = 12,
        // Broker 查询Broker Offset
        QUERY_BROKER_OFFSET_VALUE = 13,
        // Broker 查询Consumer Offset
        QUERY_CONSUMER_OFFSET_VALUE = 14,
        // Broker 更新Consumer Offset
        UPDATE_CONSUMER_OFFSET_VALUE = 15,
        // Broker 更新或者增加一个Topic
        UPDATE_AND_CREATE_TOPIC_VALUE = 17,
        // Broker 获取所有Topic的配置（Slave和Namesrv都会向Master请求此配置）
        GET_ALL_TOPIC_CONFIG_VALUE = 21,
        // Broker 获取所有Topic配置（Slave和Namesrv都会向Master请求此配置）
        GET_TOPIC_CONFIG_LIST_VALUE = 22,
        // Broker 获取所有Topic名称列表
        GET_TOPIC_NAME_LIST_VALUE = 23,
        // Broker 更新Broker上的配置
        UPDATE_BROKER_CONFIG_VALUE = 25,
        // Broker 获取Broker上的配置
        GET_BROKER_CONFIG_VALUE = 26,
        // Broker 触发Broker删除文件
        TRIGGER_DELETE_FILES_VALUE = 27,
        // Broker 获取Broker运行时信息
        GET_BROKER_RUNTIME_INFO_VALUE = 28,
        // Broker 根据时间查询队列的Offset
        SEARCH_OFFSET_BY_TIMESTAMP_VALUE = 29,
        // Broker 查询队列最大Offset
        GET_MAX_OFFSET_VALUE = 30,
        // Broker 查询队列最小Offset
        GET_MIN_OFFSET_VALUE = 31,
        // Broker 查询队列最早消息对应时间
        GET_EARLIEST_MSG_STORETIME_VALUE = 32,
        // Broker 根据消息ID来查询消息
        VIEW_MESSAGE_BY_ID_VALUE = 33,
        // Broker Client向Client发送心跳，并注册自身
        HEART_BEAT_VALUE = 34,
        // Broker Client注销
        UNREGISTER_CLIENT_VALUE = 35,
        // Broker Consumer将处理不了的消息发回服务器
        CONSUMER_SEND_MSG_BACK_VALUE = 36,
        // Broker Commit或者Rollback事务
        END_TRANSACTION_VALUE = 37,
        // Broker 获取ConsumerId列表通过GroupName
        GET_CONSUMER_LIST_BY_GROUP_VALUE = 38,
        // Broker 主动向Producer回查事务状态
        CHECK_TRANSACTION_STATE_VALUE = 39,
        // Broker Broker通知Consumer列表变化
        NOTIFY_CONSUMER_IDS_CHANGED_VALUE = 40,
        // Broker Consumer向Master锁定队列
        LOCK_BATCH_MQ_VALUE = 41,
        // Broker Consumer向Master解锁队列
        UNLOCK_BATCH_MQ_VALUE = 42,
        // Broker 获取所有Consumer Offset
        GET_ALL_CONSUMER_OFFSET_VALUE = 43,
        // Broker 获取所有定时进度
        GET_ALL_DELAY_OFFSET_VALUE = 45,
        // Namesrv 向Namesrv追加KV配置
        PUT_KV_CONFIG_VALUE = 100,
        // Namesrv 从Namesrv获取KV配置
        GET_KV_CONFIG_VALUE = 101,
        // Namesrv 从Namesrv获取KV配置
        DELETE_KV_CONFIG_VALUE = 102,
        // Namesrv 注册一个Broker，数据都是持久化的，如果存在则覆盖配置
        REGISTER_BROKER_VALUE = 103,
        // Namesrv 卸载一个Broker，数据都是持久化的
        UNREGISTER_BROKER_VALUE = 104,
        // Namesrv 根据Topic获取Broker Name、队列数(包含读队列与写队列)
        GET_ROUTEINTO_BY_TOPIC_VALUE = 105,

        // Namesrv 获取注册到Name Server的所有Broker集群信息
        GET_BROKER_CLUSTER_INFO_VALUE = 106,
        UPDATE_AND_CREATE_SUBSCRIPTIONGROUP_VALUE = 200,
        GET_ALL_SUBSCRIPTIONGROUP_CONFIG_VALUE = 201,
        GET_TOPIC_STATS_INFO_VALUE = 202,
        GET_CONSUMER_CONNECTION_LIST_VALUE = 203,
        GET_PRODUCER_CONNECTION_LIST_VALUE = 204,
        WIPE_WRITE_PERM_OF_BROKER_VALUE = 205,

        // 从Name Server获取完整Topic列表
        GET_ALL_TOPIC_LIST_FROM_NAMESERVER_VALUE = 206,
        // 从Broker删除订阅组
        DELETE_SUBSCRIPTIONGROUP_VALUE = 207,
        // 从Broker获取消费状态（进度）
        GET_CONSUME_STATS_VALUE = 208,
        // Suspend Consumer消费过程
        SUSPEND_CONSUMER_VALUE = 209,
        // Resume Consumer消费过程
        RESUME_CONSUMER_VALUE = 210,
        // 重置Consumer Offset
        RESET_CONSUMER_OFFSET_IN_CONSUMER_VALUE = 211,
        // 重置Consumer Offset
        RESET_CONSUMER_OFFSET_IN_BROKER_VALUE = 212,
        // 调整Consumer线程池数量
        ADJUST_CONSUMER_THREAD_POOL_VALUE = 213,
        // 查询消息被哪些消费组消费
        WHO_CONSUME_THE_MESSAGE_VALUE = 214,

        // 从Broker删除Topic配置
        DELETE_TOPIC_IN_BROKER_VALUE = 215,
        // 从Namesrv删除Topic配置
        DELETE_TOPIC_IN_NAMESRV_VALUE = 216,
        // Namesrv 通过 project 获取所有的 server ip 信息
        GET_KV_CONFIG_BY_VALUE_VALUE = 217,
        // Namesrv 删除指定 project group 下的所有 server ip 信息
        DELETE_KV_CONFIG_BY_VALUE_VALUE = 218,
        // 通过NameSpace获取所有的KV List
        GET_KVLIST_BY_NAMESPACE_VALUE = 219,
        // offset 重置
        RESET_CONSUMER_CLIENT_OFFSET_VALUE = 220,
        // 客户端订阅消息
        GET_CONSUMER_STATUS_FROM_CLIENT_VALUE = 221,
        // 通知 broker 调用 offset 重置处理
        INVOKE_BROKER_TO_RESET_OFFSET_VALUE = 222,
        // 通知 broker 调用客户端订阅消息处理
        INVOKE_BROKER_TO_GET_CONSUMER_STATUS_VALUE = 223,

        // Broker 查询topic被谁消费
        QUERY_TOPIC_CONSUME_BY_WHO_VALUE = 300,

        // 获取指定集群下的所有 topic
        GET_TOPICS_BY_CLUSTER_VALUE = 224,

        // 向Broker注册Filter Server
        REGISTER_FILTER_SERVER_VALUE = 301,
        // 向Filter Server注册Class
        REGISTER_MESSAGE_FILTER_CLASS_VALUE = 302,
        // 根据 topic 和 group 获取消息的时间跨度
        QUERY_CONSUME_TIME_SPAN_VALUE = 303,
        // 获取所有系统内置 Topic 列表
        GET_SYSTEM_TOPIC_LIST_FROM_NS_VALUE = 304,
        GET_SYSTEM_TOPIC_LIST_FROM_BROKER_VALUE = 305,

        // 清理失效队列
        CLEAN_EXPIRED_CONSUMEQUEUE_VALUE = 306,

        // 通过Broker查询Consumer内存数据
        GET_CONSUMER_RUNNING_INFO_VALUE = 307,

        // 查找被修正 offset (转发组件）
        QUERY_CORRECTION_OFFSET_VALUE = 308,

        // 通过Broker直接向某个Consumer发送一条消息，并立刻消费，返回结果给broker，再返回给调用方
        CONSUME_MESSAGE_DIRECTLY_VALUE = 309,

        // Broker 发送消息，优化网络数据包
        SEND_MESSAGE_V2_VALUE = 310,

        // 单元化相关 topic
        GET_UNIT_TOPIC_LIST_VALUE = 311,
        // 获取含有单元化订阅组的 Topic 列表
        GET_HAS_UNIT_SUB_TOPIC_LIST_VALUE = 312,
        // 获取含有单元化订阅组的非单元化 Topic 列表
        GET_HAS_UNIT_SUB_UNUNIT_TOPIC_LIST_VALUE = 313,
        // 克隆某一个组的消费进度到新的组
        CLONE_GROUP_OFFSET_VALUE = 314,

        // 查看Broker上的各种统计信息
        VIEW_BROKER_STATS_DATA_VALUE = 315,
    };

    enum MQResponseCode
    {
        // Broker 刷盘超时
        FLUSH_DISK_TIMEOUT_VALUE = 10,
        // Broker 同步双写，Slave不可用
        SLAVE_NOT_AVAILABLE_VALUE = 11,
        // Broker 同步双写，等待Slave应答超时
        FLUSH_SLAVE_TIMEOUT_VALUE = 12,
        // Broker 消息非法
        MESSAGE_ILLEGAL_VALUE = 13,
        // Broker, Namesrv 服务不可用，可能是正在关闭或者权限问题
        SERVICE_NOT_AVAILABLE_VALUE = 14,
        // Broker, Namesrv 版本号不支持
        VERSION_NOT_SUPPORTED_VALUE = 15,
        // Broker, Namesrv 无权限执行此操作，可能是发、收、或者其他操作
        NO_PERMISSION_VALUE = 16,
        // Broker, Topic不存在
        TOPIC_NOT_EXIST_VALUE = 17,
        // Broker, Topic已经存在，创建Topic
        TOPIC_EXIST_ALREADY_VALUE = 18,
        // Broker 拉消息未找到（请求的Offset等于最大Offset，最大Offset无对应消息）
        PULL_NOT_FOUND_VALUE = 19,
        // Broker 可能被过滤，或者误通知等
        PULL_RETRY_IMMEDIATELY_VALUE = 20,
        // Broker 拉消息请求的Offset不合法，太小或太大
        PULL_OFFSET_MOVED_VALUE = 21,
        // Broker 查询消息未找到
        QUERY_NOT_FOUND_VALUE = 22,
        // Broker 订阅关系解析失败
        SUBSCRIPTION_PARSE_FAILED_VALUE = 23,
        // Broker 订阅关系不存在
        SUBSCRIPTION_NOT_EXIST_VALUE = 24,
        // Broker 订阅关系不是最新的
        SUBSCRIPTION_NOT_LATEST_VALUE = 25,
        // Broker 订阅组不存在
        SUBSCRIPTION_GROUP_NOT_EXIST_VALUE = 26,
        // Producer 事务应该被提交
        TRANSACTION_SHOULD_COMMIT_VALUE = 200,
        // Producer 事务应该被回滚
        TRANSACTION_SHOULD_ROLLBACK_VALUE = 201,
        // Producer 事务状态未知
        TRANSACTION_STATE_UNKNOW_VALUE = 202,
        // Producer ProducerGroup错误
        TRANSACTION_STATE_GROUP_WRONG_VALUE = 203,
        // 单元化消息，需要设置 buyerId
		NO_BUYER_ID_VALUE = 204,
		// 单元化消息，非本单元消息
		NOT_IN_CURRENT_UNIT_VALUE = 205,
		// Consumer不在线
		CONSUMER_NOT_ONLINE_VALUE = 206,
		// Consumer消费消息超时
		CONSUME_MSG_TIMEOUT_VALUE = 207,
    };

	const char* getMQRequestCodeString(int code);
	const char* getMQResponseCodeString(int code);
}

#endif
