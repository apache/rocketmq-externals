# 简介

一个 Ruby 语言版本的 logstash-output-Rocketmq 插件

## 环境

Ruby 语言环境

logstash-6.6版本

## 编译安装

* 在插件目录下执行命令：
  
      gem build logstash-output-rocketmq.gemspec

* 此时插件目录下会生成一个名为 logstash-output-rocketmq-0.1.5-java.gem 的 gem 包，将该包复制粘贴到 logstash的安装目录下
  
* 在Logstash安装目录下执行命令：
  
      bin/logstash-plugin install logstash-output-rocketmq-0.1.5-java.gem

## 运行

* 启动 RocketMQ，启动 consumer

* 在 logstash\config 文件夹下，新建配置文件 rocketmq.conf，并填入以下内容：
  
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

* 在 logstash 目录下执行命令：

      bin/logstash -f config/rocketmq.conf

* 此时在命令行中输入内容，即可看到 RocketMQ 中的 consumer 消费了相应内容