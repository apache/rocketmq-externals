# Logstash Plugin

一个logstash插件，可以将logstash事件输出到rocketmq中

## 环境准备

- Ruby语言环境
- 安装好Logstash

## 插件安装

- 在插件目录下运行
 ```sh
 gem build logstash-output-rocketmq.gemspec
 ```
 - 此时插件目录下会生成一个名为logstash-output-rocketmq-0.1.0-java.gem的gem包，将此包复制粘贴到Logstash的安装目录下
 - 在Logstash安装目录下运行
 ```sh
 bin/logstash-plugin install logstash-output-rocketmq-0.1.0-java.gem
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
