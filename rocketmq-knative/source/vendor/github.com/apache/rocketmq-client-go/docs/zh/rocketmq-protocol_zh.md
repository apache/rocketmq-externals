# RocketMQ 通信协议

在 RocketMQ 中，`RemotingCommand` 是 RocketMQ 通信的基本对象，Request/Response 最后均被包装成 `RemotingCommand`。一个 `RemotingCommand` 在被序列化后的格式如下：

```
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ frame_size | header_length |         header_body        |     body     +
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+   4bytes   |     4bytes    | (21 + r_len + e_len) bytes | remain bytes +
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
```

| item | type | description |
| :-: | :-: | :-: |
| frame_size | `int32` | 一个 `RemotingCommand` 数据包大小 |
| header_length | `int32` | 高8位表示数据的序列化方式，余下的表示真实 header 长度 |
| header_body | `[]byte` | header 的 payload，长度由附带的 `remark` 和 `properties` 决定|
| body | `[]byte` | 具体 Request/Response 的 payload |

## Header

RocketMQ 的 Header 序列化方式有两种：JSON 和 RocketMQ 私有的序列化方式。JSON 序列化方式不再赘述。具体可以参考 Java `RemotingCommand` 类。

主要介绍 RocketMQ 的私有序列化方式。

在序列化的时候，需要将序列化方式记录进数据包里面，即对 `header_length` 进行编码

```go
// 编码算法

// 编码后的 header_length
var header_length int32

// 实际的 header 长度
var headerDataLen int32

// 序列化方式
var SerializedType byte

result := make([]byte, 4)
result[0]|SerializedType
result[1]|byte((headerDataLen >> 16) & 0xFF)
result[2]|byte((headerDataLen >> 8) & 0xFF)
result[3]|byte(headerDataLen & 0xFF)
binary.Read(result, binary.BigEndian, &header_length)

// 解码算法
headerDataLen := header_length & 0xFFFFFF
SerializedType := byte((header_length >> 24) & 0xFF)
```

### Header Frame

```
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+  request_code | l_flag | v_flag | opaque | request_flag |  r_len  |   r_body    |  e_len  |    e_body   +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+     2bytes    |  1byte | 2bytes | 4bytes |    4 bytes   | 4 bytes | r_len bytes | 4 bytes | e_len bytes +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
```

| item | type | description |
| :-: | :-: | :-: |
| request_code | `int16` | 哪一种 Request 或 ResponseCode，具体类别由 request_flag 决定 |
| l_flag | `byte` | language 位，用来标识Request来源方的开发语言 |
| v_flag | `int16` | 版本标记位 |
| request_flag |`int32`| Header标记位，用来标记该 `RemotingCommand` 的类型和请求方式 |
| opaque | `int32` | 标识 Request/Response 的 RequestID，Broker 返回的 Response 通过该值和 Client 缓存的 Request 一一对应 |
| r_len | `int32` | length of remark, remark 是 Request/Response 的附带说明信息，一般在 Response 中用来说明具体的错误原因 |
| r_body | `[]byte` | payload of remark |
| e_len | `int32` | length of extended fields，即 properties，一些非标准字段会存储在这里，在 RocketMQ 的各种 feature 中均有广泛应用 |
| e_body | `int32` | payload of extended fields |

## Body

`body` 是具体的 Request/Response 的数据，在 RocketMQ 中，有许多种 Request/Response。每个类有自己的序列化和反序列方式，由于种类过多，
这里就不再展开。可以具体参考Java代码中对`CommandCustomHeader`的使用。下面列一些 Client 使用到的 Request 和 Response。

### RequestCode

| item | type | description |
| :-: | :-: | :-: |
| SEND_MESSAGE | 10 | 向broker发送消息 |
| PULL_MESSAGE | 11 | 从broker拉取消息，client的push模式也是通过pull的长轮询来实现的 |
| TODO... | | |

### ResponseCode

| item | type | description |
| :-: | :-: | :-: |
| FLUSH_DISK_TIMEOUT | 10 | broker 存储层刷盘超时 |
| SLAVE_NOT_AVAILABLE | 11 | slave 节点无法服务 |
| FLUSH_SLAVE_TIMEOUT | 12 | 数据同步到 slave 超时 |
| MESSAGE_ILLEGAL | 13 | 消息格式不合格 |
| SERVICE_NOT_AVAILABLE | 14 | broker 暂时不可用 |
| VERSION_NOT_SUPPORTED | 15 | 不支持的请求，目前没有看到使用 |
| NO_PERMISSION | 16 | 对 broker、topic 或 subscription 无访问权限 |
| TOPIC_EXIST_ALREADY | 18 | topic 已存在，目前没看到使用 |
| PULL_NOT_FOUND | 19 | 没拉到消息，大多为 offset 错误 |
| PULL_RETRY_IMMEDIATELY | 20 | 建议 client 立即重新拉取消息 |
| PULL_OFFSET_MOVED | 21 | offset 太小或太大 |
| QUERY_NOT_FOUND | 22 | 管理面 Response，TODO |
| SUBSCRIPTION_PARSE_FAILED | 23 | 订阅数据解析失败 |
| SUBSCRIPTION_NOT_EXIST | 24 | 订阅不存在 |
| SUBSCRIPTION_NOT_LATEST | 25 | 订阅数据版本和 request 数据版本不匹配 |
| SUBSCRIPTION_GROUP_NOT_EXIST | 26 | 订阅组不存在 |
| FILTER_DATA_NOT_EXIST | 27 | filter 数据不存在 |
| FILTER_DATA_NOT_LATEST | 28 | filter 数据版本和 request 数据版本不匹配 |
| TRANSACTION_SHOULD_COMMIT | 200 | 事务 Response，TODO |
| TRANSACTION_SHOULD_ROLLBACK | 201 | 事务 Response，TODO |
| TRANSACTION_STATE_UNKNOW | 202 | 事务 Response，TODO | |
| TRANSACTION_STATE_GROUP_WRONG | 203 | 事务 Response，TODO |
| NO_BUYER_ID | 204 | 不知道是什么，没看到 broker 端在使用 |
| NOT_IN_CURRENT_UNIT | 205 | 不知道是什么，没看到 broker 端在使用 |
| CONSUMER_NOT_ONLINE | 206 | consumer 不在线，控制面 response |
| CONSUME_MSG_TIMEOUT | 207 | client request 等待 broker 相应超时 |
| NO_MESSAGE | 208 | 控制面 response，由 client 自己设置，不清楚具体用途 |
