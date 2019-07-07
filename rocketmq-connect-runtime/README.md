# rocketmq-connector 
[toc]
# 启动参数选择


参数 | 说明
---|---
-h| 打印日志
-c | 启动参数配置文件路径

# 快速开始

## 1.准备

1. 64位  OS, Linux/Unix/Mac操作系统

2. 64bit JDK 1.8+

3. Maven 3.2.x或以上版本

4. Git

5. 磁盘空间1G


## 2.下载

1. 下载[openmessaging-connect](https://github.com/openmessaging/openmessaging-connect/archive/master.zip)

2. 下载 [rocketmq-externals](https://github.com/apache/rocketmq-externals/archive/master.zip)
 
## 3.编译

1. 将openmessaging-connect编译并安装到本地maven文件库中


```
> unzip openmessaging-connect-master.zip -d ./
> cd openmessaging-connect-master
> mvn install
```
2. 编译rocketmq-connect-runtime

```
> unzip rocketmq-externals-master.zip -d ./
> cd rocketmq-externals-master/rocketmq-connect-runtime
> mvn -Prelease-all -DskipTests clean install -U
> cd targer/distribution/conf
```

## 4.配置

1. 修改配置文件connect.conf


```
workerId=DEFAULT_WORKER_1

## Http prot for user to access REST API
httpPort=8081

## local file dir for config store
storePathRootDir=/home/connect/storeRoot

## 改成自己的
namesrvAddr=127.0.0.1:9876  

pluginPaths=/home/connect/file-connect/target
``` 

## 5.运行

返回recoketmq-connect-runtime根目录运行
```
> nohup ./run_worker.sh &

run rumtime worker
The worker [DEFAULT_WORKER_1] boot success.
```

## 6.日志（3个日志文件到底记录了啥东西）

1. ${user.home}/logs/rocketmqconnect/connect_runtime.log 服务器运行日志
2. ${user.home}/logs/rocketmqconnect/connect_default.log 
3. ${user.home}/logs/rocketmqlogs/rocketmq_client.log 客户端日志

## 7.配置文件(3个配置文件到底啥作用)

配置文件根据配置，存放，默认存放在 /home/connect/storeRoot

配置文件为 
1. connectorConfig.json 连接配置文件
2. position.json 位置配置文件
3. taskConfig.json 任务配置文件


