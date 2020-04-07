---
description: 一个简单的文件同步connector
---

# RocketMQ file-connector

## 拷贝流程

![&#x6587;&#x4EF6;&#x4F20;&#x8F93;&#x603B;&#x89C8;](../.gitbook/assets/file-connect-overview-2.png)

首先有个源文件，文件中保存有一些数据。启动Runtime，通过RESTful接口启动Source和Sink Connector，启动完成以后Source Connector会读取文件内容发送到RocketMQ指定的Topic中，Sink Connector会从这个Topic拉去数据写入指定文件中，这样就完成从源数据存储到目标存储之间的数据同步。

这里只是个简单的文件拷贝，其它数据源流程也是类似，通过RocketMQ Connect几乎可以实现任意数据源之间数据同步，包括异构数据源之间数据同步。

## 项目安装使用

### 环境依赖

1. **64bit JDK 1.8+;**
2. **Maven 3.2.x或以上版本;**
3. **两套RocketMQ集群环境;** 
4. **RocketMQ Runtime环境**

### **项目构建**

`file-connector` 是`runtime`的样例Connector，所以在构建`runtime`时已经构建过，具体参照前一节`runtime`构建过程。

#### [runtime项目构建](rocketmq-runtime-shi-yong.md#xiang-mu-gou-jian)

### 项目安装

{% hint style="warning" %}
建议将connector插件放于一个公共的目录下，推荐为

**/usr/local/connector-plugins/**
{% endhint %}

* 将jar包拷贝到connector插件目录下

```bash
# 进入到rocketmq-connect-sample所在的目录
$ cd {rocketmq-external目录}/rocketmq-connect/rocketmq-connect-sample/
# 进入jar包所在的目录
$ cd target
# 将jar包拷贝到connector插件目录下
$ cp rocketmq-connect-sample-0.0.1-SNAPSHOT.jar /usr/local/connector-plugins/
```

* 启动runtime，具体参照前一节的项目启动

     [runtime项目运行](rocketmq-runtime-shi-yong.md#rocketmq-runtime-pei-zhi-wen-dang)

## 启动Source Connector

### 启动前准备

* 在`/opt/`文件下创建`source-file`文件夹\(当然也可以自主选择合适的位置\)

```bash
$ cd /opt/
$ mkdir source-file
$ cd source-file
```

* 创建一个`source-file.txt`用于测试

```bash
$ vim source-file.txt
# 在其中写入一些文字，并保存
```

*  创建Topic

```bash
# 创建Topic
# 注意这里的ip地址要换成自己服务器ip
$ sh mqadmin updateTopic -b 4xx.1xx.2xx.2xx:10911 -t fileTopic
....
create topic to 4xx.1xx.2xx.2xx:10911 success.

```

更多创建Topic内容参照 [创建Topic](rocketmq-runtime-shi-yong.md#chuang-jian-topic)

### Source Connector启动

#### 启动模板

```typescript
# GET请求  
http://(worker ip):(port)/connectors/(connector name)?config={
"connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector",
"topic":"fileTopic",
"filename":"/opt/source-file/source-file.txt",
"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
}   

```

#### 参数说明

<table>
  <thead>
    <tr>
      <th style="text-align:left">&#x53C2;&#x6570;</th>
      <th style="text-align:left">&#x542B;&#x4E49;</th>
      <th style="text-align:left">&#x80FD;&#x5426;&#x4E3A;&#x7A7A;</th>
      <th style="text-align:left">&#x793A;&#x4F8B;</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="text-align:left">worker ip</td>
      <td style="text-align:left">runtime&#x4E0A;wokerde&#x542F;&#x52A8;ip</td>
      <td style="text-align:left">false</td>
      <td style="text-align:left">127.0.0.1</td>
    </tr>
    <tr>
      <td style="text-align:left">port</td>
      <td style="text-align:left">&#x7AEF;&#x53E3;&#x53F7;&#xFF0C;&#x53EF;&#x4EE5;&#x5728;&#x914D;&#x7F6E;&#x9879;&#x4FEE;&#x6539;&#xFF0C;&#x9ED8;&#x8BA4;&#x4E3A;8081</td>
      <td
      style="text-align:left">false</td>
        <td style="text-align:left">8081</td>
    </tr>
    <tr>
      <td style="text-align:left">connector name</td>
      <td style="text-align:left">
        <p>connector&#x914D;&#x7F6E;&#x7684;&#x552F;&#x4E00;key&#xFF0C;</p>
        <p>&#x9664;&#x4FDD;&#x7559;&#x5B57;`stopAll`&#x5916;&#x7684;&#x4EFB;&#x4E00;&#x5B57;&#x7B26;&#x4E32;&#xFF0C;</p>
        <p>&#x4F46;&#x662F;connector&#x793A;&#x4F8B;&#x4E4B;&#x95F4;&#x4E0D;&#x53EF;&#x4EE5;&#x91CD;&#x540D;</p>
      </td>
      <td style="text-align:left">false</td>
      <td style="text-align:left">file-connector</td>
    </tr>
  </tbody>
</table>#### 配置说明

<table>
  <thead>
    <tr>
      <th style="text-align:left">key</th>
      <th style="text-align:left">description</th>
      <th style="text-align:left">&#x80FD;&#x5426;&#x4E3A;&#x7A7A;</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="text-align:left">connector-class</td>
      <td style="text-align:left">&#x5B9E;&#x73B0;Connector&#x63A5;&#x53E3;&#x7684;&#x7C7B;&#x540D;&#x79F0;&#xFF08;&#x5305;&#x542B;&#x5305;&#x540D;&#xFF09;</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">filename</td>
      <td style="text-align:left">&#x6570;&#x636E;&#x6E90;&#x6587;&#x4EF6;&#x540D;&#x79F0;</td>
      <td style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">task-class</td>
      <td style="text-align:left">&#x5B9E;&#x73B0;SourceTask&#x7C7B;&#x540D;&#x79F0;&#xFF08;&#x5305;&#x542B;&#x5305;&#x540D;&#xFF09;</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">topic</td>
      <td style="text-align:left">&#x540C;&#x6B65;&#x6587;&#x4EF6;&#x6570;&#x636E;&#x6240;&#x9700;topic</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">update-timestamp</td>
      <td style="text-align:left">&#x914D;&#x7F6E;&#x66F4;&#x65B0;&#x65F6;&#x95F4;&#x6233;</td>
      <td style="text-align:left">true</td>
    </tr>
    <tr>
      <td style="text-align:left">source-record-converter</td>
      <td style="text-align:left">
        <p>&#x7528;&#x4E8E;&#x5C06;SourceDataEntry&#x8F6C;&#x6362;&#x4E3A;byte[]&#x7684;&#x8F6C;&#x6362;&#x5668;</p>
        <p>&#x5B9E;&#x73B0;&#x7684;&#x5B8C;&#x6574;&#x7C7B;&#x540D;</p>
      </td>
      <td style="text-align:left">false</td>
    </tr>
  </tbody>
</table>#### 启动示例

```javascript
http://localhost:8081/connectors/fileConnectorSource?config={
"connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector",
"topic":"fileTopic",
"filename":"/opt/source-file/source-file.txt",
"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
}
```

在浏览器上使用上面请求（或者使用其他工具发送GET请求），可以看到浏览器返回`success`，并且控制台输出以下信息即表示成功

```bash
2019-11-05 13:34:49 INFO qtp726181440-29 - config: {
"connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector",
"topic":"fileTopic",
"filename":"/opt/source-file/source-file.txt",
"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
}

```

## 启动Sink Connector

### 启动前准备

{% hint style="info" %}
注意：启动**Source Connector**之前已经在rocketmq集群上创建了Topic即**fileTopic，**所以Sink Connector 不需要再创建Topic
{% endhint %}

### Sink Connector 启动

#### 启动模板

```typescript
 # GET请求  
 http://(your worker ip):(port)/connectors/(connector name)?config={
    "connector-class":"org.apache.rocketmq.connect.file.FileSinkConnector",
    "topicNames":"fileTopic",
    "filename":"/opt/sink-files/sink-file.txt",
    "source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
 }
```

#### 参数说明

参照Source Connector [参数说明](file-connector.md#can-shu-shuo-ming)

#### 配置说明

<table>
  <thead>
    <tr>
      <th style="text-align:left">key</th>
      <th style="text-align:left">description</th>
      <th style="text-align:left">&#x80FD;&#x5426;&#x4E3A;&#x7A7A;</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="text-align:left">connector-class</td>
      <td style="text-align:left">&#x5B9E;&#x73B0;Connector&#x63A5;&#x53E3;&#x7684;&#x7C7B;&#x540D;&#x79F0;&#xFF08;&#x5305;&#x542B;&#x5305;&#x540D;&#xFF09;</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">topicNames</td>
      <td style="text-align:left">sink&#x9700;&#x8981;&#x5904;&#x7406;&#x6570;&#x636E;&#x6D88;&#x606F;topics</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">task-class</td>
      <td style="text-align:left">&#x5B9E;&#x73B0;SourceTask&#x7C7B;&#x540D;&#x79F0;&#xFF08;&#x5305;&#x542B;&#x5305;&#x540D;&#xFF09;</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">filename</td>
      <td style="text-align:left">sink&#x62C9;&#x53BB;&#x7684;&#x6570;&#x636E;&#x4FDD;&#x5B58;&#x5230;&#x6587;&#x4EF6;</td>
      <td
      style="text-align:left">false</td>
    </tr>
    <tr>
      <td style="text-align:left">update-timestamp</td>
      <td style="text-align:left">&#x914D;&#x7F6E;&#x66F4;&#x65B0;&#x65F6;&#x95F4;&#x6233;</td>
      <td style="text-align:left">true</td>
    </tr>
    <tr>
      <td style="text-align:left">source-record-converter</td>
      <td style="text-align:left">
        <p>&#x7528;&#x4E8E;&#x5C06;SourceDataEntry&#x8F6C;&#x6362;&#x4E3A;byte[]&#x7684;&#x8F6C;&#x6362;&#x5668;</p>
        <p>&#x5B9E;&#x73B0;&#x7684;&#x5B8C;&#x6574;&#x7C7B;&#x540D;</p>
      </td>
      <td style="text-align:left">false</td>
    </tr>
  </tbody>
</table>#### 启动示例

```typescript
 http://localhost:8081/connectors/fileConnectorSink?config={
    "connector-class":"org.apache.rocketmq.connect.file.FileSinkConnector",
    "topicNames":"fileTopic",
    "filename":"/opt/sink-files/sink-file.txt",
    "source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
 }
```

在浏览器上使用上面请求（或者使用其他工具发送GET请求），可以看到浏览器返回`success`，并且控制台输出以下信息即表示成功。

```bash
2019-11-05 18:48:51 INFO qtp726181440-26 - fileConnectorSink?config={
    "connector-class":"org.apache.rocketmq.connect.file.FileSinkConnector",
    "topicNames":"fileTopic",
    "filename":"/opt/sink-files/sink-file.txt",
    "source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
 }

```

如果rocketmq集群没有过多的持久化消息，可以看到相同内容的文件`/opt/sink-files/sink-file.txt`

