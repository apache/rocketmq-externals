# logstash-output-rocketmq

Logstash plugin output to Rocketmq

## Installation

* If the installation environment has internet (Refer to [Logstash output plugin test installation](https://www.elastic.co/guide/en/logstash/current/_how_to_write_a_logstash_output_plugin.html#_test_installation_4))

  * Place the jar file in rocketmq_jar in /vendor/jar/rocketmq in the installation directory of Logstash

  * Put logstash-output-rocketmq-x.x.x.gem in the installation directory of Logstash

  * Run `bin/logstash-plugin install logstash-output-rocketmq-x.x.x.gem` in the installation directory of Logstash

* If the installation environment dose not have internet (Refer to [Logstash installing offline plugin packs](https://www.elastic.co/guide/en/logstash/current/offline-plugins.html#installing-offline-packs))

  * Place the jar file in rocketmq_jar in /vendor/jar/rocketmq in the installation directory of Logstash

  * Put logstash-offline-plugins-6.4.0.zip in the installation directory of Logstash

  * Run `bin/logstash-plugin install file:///path/to/logstash-offline-plugins-6.4.0.zip` in the installation directory of Logstash

## Configurations

|Option|Type|Description|Required?|Default|
|---|---|---|---|---|
|logstash_path|String|The installation directory of Logstash, e.g. C:/ELK/logstash, /usr/local/logstash|Yes||
|name_server_addr|String|Rocketmq's NameServer address, e.g. 192.168.10.10:5678|Yes||
|producer_group|String|Rocketmq's producer group|No|defaultProducerGroup|
|use_vip_channel|boolean|if Rocketmq use VIP channel|No|false|
|topic|String|Message's topic|Yes||
|topic_format|boolean|is topic need to use formatting|No|false|
|tag|String|Message's tag|No|defaultTag|
|tag_format|boolean|is tag need to use formatting|No|false|
|key|String|Message's key|No|defaultKey|
|key_format|boolean|is key need to use formatting|No|false|
|body|String|Message's body|No||
|body_format|boolean|is body need to use formatting|No|false|
|retry_times|Number|Number of retries after failed delivery|No|2|
|codec|Object|codec plugin config|No|plain|

## Rewrite & Rebuild

The core file is [rocketmq.rb](https://github.com/PriestTomb/logstash-output-rocketmq/blob/master/lib/logstash/outputs/rocketmq.rb), if you have modified this file, you can run `gem build logstash-output-rocketmq.gemspec` to rebuild the gem file, or you can run `bin/logstash-plugin prepare-offline-pack logstash-output-rocketmq` to rebuild the offline packs (Refer to [Logstash building offline plugin packs](https://www.elastic.co/guide/en/logstash/current/offline-plugins.html#building-offline-packs))