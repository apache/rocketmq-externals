# logstash-output-rocketmq

Logstash output 插件的 Rocketmq 版

[README](https://github.com/PriestTomb/logstash-output-rocketmq/blob/master/README.md)

## 安装

* 如果安装环境有网络（参考[ Logstash 插件测试安装](https://www.elastic.co/guide/en/logstash/current/_how_to_write_a_logstash_output_plugin.html#_test_installation_4)）

  * 将 rocketmq_jar 中的 jar 文件放到 Logstash 安装目录下的 /vendor/jar/rocketmq 中
  * 将 logstash-output-rocketmq-x.x.x.gem 放到 Logstash 的安装目录下
  * 在 Logstash 的安装目录下执行 `bin/logstash-plugin install logstash-output-rocketmq-x.x.x.gem`

* 如果安装环境无网络（参考[ Logstash 插件离线安装](https://www.elastic.co/guide/en/logstash/current/offline-plugins.html#installing-offline-packs)）

  * 将 rocketmq_jar 中的 jar 文件放到 Logstash 安装目录下的 /vendor/jar/rocketmq 中
  * 将 logstash-offline-plugins-6.4.0.zip 放到 Logstash 的安装目录下
  * 在 Logstash 的安装目录下执行 `bin/logstash-plugin install file:///path/to/logstash-offline-plugins-6.4.0.zip`

## 配置参数

|参数|类型|描述|是否必需|默认值|
|---|---|---|---|---|
|logstash_path|String|本地 Logstash 的路径，如 C:/ELK/logstash、/usr/local/logstash|是||
|name_server_addr|String|Rocketmq 的 NameServer 地址，如 192.168.10.10:5678|是||
|producer_group|String|Rocketmq 的 producer group|否|defaultProducerGroup|
|use_vip_channel|boolean|Rocketmq 是否使用 VIPChannel|否|false|
|topic|String|Message 的 topic|是||
|topic_format|boolean|topic 是否需要格式化|否|false|
|tag|String|Message 的 tag|否|defaultTag|
|tag_format|boolean|tag 是否需要格式化|否|false|
|key|String|Message 的 key|否|defaultKey|
|key_format|boolean|key 是否需要格式化|否|false|
|body|String|Message 的 body|否||
|body_format|boolean|body 是否需要格式化|否|false|
|retry_times|Number|发送异常后的重试次数|否|2|
|codec|Object|Logstash 的 codec 插件配置|否|plain|

## 重写编译

插件的核心文件仅为 [rocketmq.rb](https://github.com/PriestTomb/logstash-output-rocketmq/blob/master/lib/logstash/outputs/rocketmq.rb)，如果有修改，可重新使用 `gem build logstash-output-rocketmq.gemspec` 编译 gem 文件，或使用 `bin/logstash-plugin prepare-offline-pack logstash-output-rocketmq` 打包离线插件包（参考[ Logstash 打包离线插件包](https://www.elastic.co/guide/en/logstash/current/offline-plugins.html#building-offline-packs)）