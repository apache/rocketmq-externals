# rocketmq-connect-hudi

## rocketmq-connect-hudi 打包
```
mvn clean install -DskipTest -U 
```
将target目录下打包的rocketmq-connect-hudi-0.0.1-SNAPSHOT-jar-with-dependencies.jar拷贝到connector-runtime connect.conf配置的connector-plugin目录下。
## 目前安装会遇到的问题

目前的rocketmq-connect-hudi 使用的是0.8.0版本的hudi.

## rocketmq-connect-hudi 启动

首先，需要启动connect-runtime，参考rocketmq-connect-runtime的run_work.sh脚本。
* **hudi-sink-connector** 启动

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-hudi-sink-connector-name}
?config='{"connector-class":"org.apache.rocketmq.connect.hudi.connector.HudiSinkConnector","topicNames":"topicc","tablePath":"file:///tmp/hudi_connector_test","tableName":"hudi_connector_test_table","insertShuffleParallelism":"2","upsertShuffleParallelism":"2","deleteParallelism":"2","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.RocketMQConverter","source-rocketmq":"127.0.0.1:9876","src-cluster":"DefaultCluster","refresh-interval":"10000","schemaPath":"/Users/osgoo/Downloads/user.avsc"\}'
```
启动成功会打印如下日志：
```
2021-09-06 16:23:14 INFO pool-2-thread-1 - Open HoodieJavaWriteClient successfully
```
>**注：** `rocketmq-hudi-connect` 的启动依赖于`rocketmq-connect-runtime`项目的启动，需将打好的`jar`包放置到`runtime`项目中`pluginPaths`配置的路径后再执行上面的启动请求,该值配置在`runtime`项目下的`connect.conf`文件中

## rocketmq-connect-hudi 停止

```
http://${runtime-ip}:${runtime-port}/connectors/${rocketmq-hudi-connector-name}/stop
```

## rocketmq-connect-hudi 参数说明
* **hudi-sink-connector 参数说明**

参数 | 类型 | 是否必须 | 描述 | 样例
|---|---|---|---|---|
|connector-class | String | 是 | sink connector类 | HudiSinkConnector|
|tablePath | String | 是 | sink到hudi的表路径 | file:///tmp/hudi_connector_test |
|tableName | String | 是 | sink到hudi的表名称| hudi_connector_test_table |
|insertShuffleParallelism | int | 是 | hudi insert并发度 | 2 |
|upsertShuffleParallelism | int | 是 | hudi upsert并发度 | 2 |
|deleteParallelism | int | 是 | hudi delete并发度 | 2 |
|topicNames | String | 是 | rocketmq默认每一个数据源中的表对应一个名字，该名称需和数据库表名称相同 | jdbc_hudi |
|task-divide-strategy | Integer | 否 | task 分配策略, 默认值为 0，表示按照topic分配任务，每一个table便是一个topic | 0 |
|task-parallelism | Integer | 否 | task parallelism，默认值为 1，表示将topic拆分为多少个任务进行执行 | 2 |
|source-cluster | String | 是 | sink 端 RocketMQ cluster 名称 | DefaultCluster |
|source-rocketmq | String | 是 | sink 端获取路由信息连接到的 RocketMQ nameserver 地址 | 127.0.0.1:9876 |
|source-record-converter | String | 是 | source data 解析 | org.apache.rocketmq.connect.runtime.converter.RocketMQConverter |
|refresh-interval | String | 否 | sink的刷新时间，单位ms | 10000 |
|schemaPath | String | 是 | sink的schema地址 | /Users/osgoo/Downloads/user.avsc" |


示例配置如下
```js
{
	"connector-class": "org.apache.rocketmq.connect.hudi.connector.HudiSinkConnector",
	"topicNames": "topicc",
	"tablePath": "file:///tmp/hudi_connector_test",
	"tableName": "hudi_connector_test_table",
	"insertShuffleParallelism": "2",
	"upsertShuffleParallelism": "2",
	"deleteParallelism": "2",
	"source-record-converter": "org.apache.rocketmq.connect.runtime.converter.RocketMQConverter",
	"source-rocketmq": "127.0.0.1:9876",
	"source-cluster": "DefaultCluster",
	"refresh-interval": "10000",
	"schemaPath": "/Users/osgoo/Downloads/user.avsc"
}
```

* **spark-submit 启动任务**
将connect-runtime打包后通过spark-submit提交任务
```
nohup sh spark-submit 	--class org.apache.rocketmq.connect.runtime.ConnectStartup --conf "spark.driver.extraJavaOptions=-Dlogback.configurationFile=logback.xml" --files /xxx/conf/connect.conf,/xxx/conf/log4j.properties  --packages org.apache.hudi:hudi-spark3-bundle_2.12:0.8.0,org.apache.spark:spark-avro_2.12:3.0.1,org.apache.hudi:hudi-java-client:0.8.0,org.apache.parquet:parquet-avro:1.10.1,org.apache.avro:avro:1.10.2,com.alibaba:fastjson:1.2.51,org.reflections:reflections:0.9.11,org.apache.httpcomponents:httpclient:4.5.5,io.openmessaging:openmessaging-connector:0.1.1,commons-cli:commons-cli:1.1,org.apache.rocketmq:rocketmq-client:4.4.0,org.apache.rocketmq:rocketmq-tools:4.4.0,org.apache.rocketmq:rocketmq-remoting:4.4.0,org.apache.rocketmq:rocketmq-openmessaging:4.3.2,org.slf4j:slf4j-api:1.7.7,com.google.guava:guava:20.0,org.apache.hadoop:hadoop-common:3.3.1,org.reflections:reflections:0.9.12,org.apache.hive:hive-exec:2.3.7 --conf 'spark.executor.userClassPathFirst=true'  --conf 'spark.driver.userClassPathFirst=true' --conf 'spark.serializer=org.apache.spark.serializer.KryoSerializer' /xxx/rocketmq/rocketmq-connect-runtime-0.0.1-SNAPSHOT.jar  &
```
后续操作参考rocketmq-connect-hudi启动步骤