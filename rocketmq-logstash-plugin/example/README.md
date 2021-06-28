# 示例及说明

结合目前学习到的内容，放几个示例配置文件在这里，加上一些说明描述，用起来更方便吧。。

## 0. simple.conf

```
output {
	rocketmq {
		codec => plain {
			format => "%{message}"
		}
		logstash_path => "C:\ELK\Logstash"
		name_server_addr => "192.168.10.10:5678"
		topic => "myTopic"
	}
}
```

一个最简单的配置，MQ 的地址为 192.168.10.10:5678，消息发送到 myTopic 这个 Topic 下，其余的配置都使用默认值

关于 codec 插件的配置，说明一下：

**Logstash 中处理的消息为 event 对象，该对象除了采集到的数据外，还有一些其他字段，比如时间戳、主机信息，所以这里如果不配置 codec 插件的话，在 MQ 服务器上可以看到最终写入的消息前面会有一串时间戳和一个 host 信息**

使用 `format => "%{message}"`，就可以过滤掉这些信息，只保留 Logstash 收集到的数据

## 1. with_filter_and_format.conf

```
filter {
	json {
		source => "message"
	}
}

output {
	if ![paramA] {
		file {
			codec => line {format => "%{message}"}
			path => "C:\ELK\Logstash\logs\no_paramA.log"
			gzip => false
		}
	} else {
		rocketmq {
			codec => plain {
				format => "%{message}"
			}
			logstash_path => "C:\ELK\Logstash"
			name_server_addr => "172.16.40.228:9876"
			producer_group => "testProducerGroup"
			topic => "plugin_test_new"
			key => "%{paramA}"
			key_format => true
			tag => "plugin_test_tag"
			#body => "%{message}"
			#body_format => true
		}
	}
}
```

考虑到 Rocketmq 中消息的其他字段，如 topic、key、tag、body，希望能根据不同的消息内容做不同的操作，如写入不同的 topic、指定更有意义的 key 等，这几个字段可以使用格式化配置，更灵活控制消息的写入

上述配置的前提是采集的数据为 json 格式，使用 json 插件，对接收到的消息进行过滤操作，将其中的各字段解析出来，在 output 环节，先判断解析后的数据有没有 paramA 这个字段，如果没有，使用 file 插件写入本地日志文件，如果有，使用 rocketmq 插件发送到 MQ

Message 的 key 字段使用了格式化，格式化的格式为 `%{xxx}`，这样就可以使用 json 插件解析出的 paramA 字段作为 MQ 消息的 key，同理，body 也可以使用 `%{message}` 的形式进行格式化，效果和使用 codec 插件一样
