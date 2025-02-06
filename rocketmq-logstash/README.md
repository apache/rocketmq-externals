# Logstash Plugin

一个logstash插件，可以将logstash事件输出到rocketmq中

## 环境准备
- Linux或者windows系统均可
- jkd1.8
- Ruby语言环境
- Logstash版本建议使用7.9.1及以上（该版本以下未做测试）
- rocketmq版本建议使用4.8.0及以上（该版本以下未做测试）

## 插件安装

- 在插件目录下运行以下命令
 ```sh
 gem build logstash-output-rocketmq.gemspec
 ```
 - 此时插件目录下会生成一个名为logstash-output-rocketmq-0.1.0-java.gem的gem包，将此包复制粘贴到Logstash的安装目录下
 - 在Logstash安装目录下运行以下命令，即可完成插件安装
 ```sh
 bin/logstash-plugin install logstash-output-rocketmq-0.1.0-java.gem
 ```
## 插件配置文件说明

完整的插件配置如下
```sh
output {
	rocketmq {
		#配置codec
		codec => plain {
			format => "%{message}"
		}
		#配置nameserver的地址和端口号，该配置项不可省略
		name_server_addr => "ip:port"
		#配置producer group，默认值为defaultProducerGroup，该配置项可以省略
		producer_group => "defaultProducerGroup"
		#配置topic，该配置项不可省略
		topic => "topic"
		#配置tag，默认值为defaultTag，该配置项可以省略
		tag => "defaultTag"
		#配置key，默认值为defaultKey，该配置项可以省略
		key => "defaultKey"
		#配置消息重发次数，默认两次，该配置项可以省略
		retryTimes => 2
	}

}
```
## 测试

- 启动好rocketmq，consumer准备好消费消息
- cd到Logstash的安装目录下，在config文件夹中新建配置文件rocketmq.conf，文件内容为
 ```sh
 input {
	stdin{}
}

output {
	rocketmq {
		codec => plain {
			format => "%{message}"
		}
		name_server_addr => "localhost:9876"
		topic => "testTopic"
	}

}
 ```
- 运行命令
```sh
 bin/logstash -f config/rocketmq.conf
```
- 在命令行中输入消息，此时rocketmq的消费者会消费到对应的消息
