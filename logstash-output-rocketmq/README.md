# Logstash Java Plugin

This is a Java plugin for logstash-rocketmq

**环境**：

Logstash-7.9.4

Java 1.8

### **使用**：

使用如下命令获取logstash-7.9

```shell
git clone --branch <branch_name> --single-branch https://github.com/elastic/logstash.git
```

`branch_name`为版本号

**生成.jar文件**

在本地的logstash根目录($LS_HOME)下执行`./gradlew assemble`，此时会生成`\$LS_HOME/logstash-core/build/libs/logstash-core-x.y.z.jar`，x，y，z对应logstash的版本号。

**clone当前项目**

clone当前项目到本地，并在项目根目录下新建文件`gradle.properties`，该文件内容为：

> ```txt
> LOGSTASH_CORE_PATH=<target_folder>/logstash-core
> ```

target_folder为本地的logstash根目录

**运行Gradle打包任务**

在本项目的根目录下执行`./gradlew gem`，此时会在项目根目录下生成一个`.gem`文件，名称为`logstash-{plugintype}-<pluginName>-<version>.gem`

**在logstash中安装该插件**

在本地的logstash根目录下运行

```shell
bin/logstash-plugin install --no-verify --local path
```

`path`为上一步生成的`.gem`文件的路径

**启动RocketMQ**

启动nameserver和broker

**运行该logstash插件**

首先编写配置文件`rocketmq.conf`，内容如下

```
input {
	stdin {}
}

output {
	rocketmq{
		namesrv-addr => "127.0.0.1:9876"
		topic => "test"
	}
}
```

执行以下命令运行logstash

```
bin/logstash -f path_to_rocketmq.conf
```

`path_to_rocketmq.conf`表示`rocketmq.conf`在本地的位置

在控制台输入消息，即可发送到rocketmq中

### 参数说明

|     参数     | 是否必须 |        默认值        |                      说明                       |
| :----------: | :------: | :------------------: | :---------------------------------------------: |
| namesrv-addr |    是    |          无          |        nameserver地址，如127.0.0.1:9876         |
|    topic     |    是    |          无          |                      topic                      |
|  group-name  |    否    | defaultProducerGroup |                 指定group name                  |
|     tag      |    否    |      defaultTag      |                   指定消息tag                   |
|     send     |    否    |        oneway        | 指定同步/异步/单向发送，可选值sync/async/oneway |
| send-timeout |    否    |        3000ms        |                  发送超时时间                   |
| retry-times  |    否    |          2           |                发送失败重试次数                 |

配置举例

example 1

```
output {
	rocketmq{
		namesrv-addr => "127.0.0.1:9876"
		topic => "test"
	}
}
```

example 2

```
output {
	rocketmq{
		namesrv-addr => "127.0.0.1:9876"
		topic => "test"
		group-name => "testGroup"
		tag => "testTag"
		send => "sync"
		send-timeout => "2000"
		retry-times => "3"
	}
}
```


