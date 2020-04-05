---
description: connector 创建、启动、停止；集群的信息获取
---

# RESTful 接口

## RESTful API

{% api-method method="get" host="http://{ip地址}:{port}/connectors" path="/:connector-name?config=:config" %}
{% api-method-summary %}
 请求模板
{% endapi-method-summary %}

{% api-method-description %}

{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="ip地址" type="string" required=true %}
runtime 运行的IP地址
{% endapi-method-parameter %}

{% api-method-parameter name="port" type="string" required=true %}
端口号，默认为8081
{% endapi-method-parameter %}

{% api-method-parameter name="connector名称" type="string" required=true %}
一个唯一的标识，stopAll除外
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}

{% api-method-query-parameters %}
{% api-method-parameter name="config" type="object" required=false %}
　　connector实例的具体参数
{% endapi-method-parameter %}
{% endapi-method-query-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
返回结果
{% endapi-method-response-example-description %}

```
success
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% hint style="warning" %}
以下API为了表示方便，进行以下规定，具体的部署请对应设置请求模板即可

* **ip = localhost**
* **port = 8081**
* **connector-name = connector-example**
* **config = {**"connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector","topic":"fileTopic","filename":"/opt/source-file/source-file.txt"**}**
{% endhint %}

 更多Connector参数请具体参考对应Connector的设置说明

* 样例1 [file-connector参数说明](../../quick-start/file-connector.md#can-shu-shuo-ming)
* 样例2 [replicator参数说明](../../rocketmq-connector/replicator/replicator-can-shu-pei-zhi.md)

{% api-method method="get" host="http://localhost:8081/connectors" path="/:connector-name?config=:config" %}
{% api-method-summary %}
启动或创建一个connector
{% endapi-method-summary %}

{% api-method-description %}
**启动一个存在的connector或者创建一个新的connector并启动**  
注：这里将模板的参数已经赋值
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="connector-name" type="string" required=true %}
connector名称，如上文的connector-example
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}

{% api-method-query-parameters %}
{% api-method-parameter name="config" type="object" required=true %}
Connector具体参数,如上文的config
{% endapi-method-parameter %}
{% endapi-method-query-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
返回结果
{% endapi-method-response-example-description %}

```markup
<!-- 如果成功 -->
success

<!-- 如果存在同名Connector -->
Connector with same config already exist.

<!-- 失败 -->
fail
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081/connectors" path="/:connector-name/stop" %}
{% api-method-summary %}
停止一个connector
{% endapi-method-summary %}

{% api-method-description %}
**停止一个运行的connector并删除对应的设置**
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="connector-name" type="string" required=true %}
connector名称
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
如果一个connector存在则停止删除，否则不进行操作
{% endapi-method-response-example-description %}

```
success
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081/connectors" path="/:connector-name/status" %}
{% api-method-summary %}
获取connector状态
{% endapi-method-summary %}

{% api-method-description %}
获取一个connector的状态
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="connector-name" type="string" required=true %}
connector名称
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
返回的状态有两种
{% endapi-method-response-example-description %}

```markup
<!-- 如果在运行 -->
runing

<!-- 如果没在运行-->
not runing
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081/" path="plugin/reload" %}
{% api-method-summary %}
重新加载plugin目录下的Connector文件
{% endapi-method-summary %}

{% api-method-description %}
重新加载plugin目录下的Connector文件，用于在runtime运行过程中新增Connector实例
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
重新扫描plugin目录，加载类文件
{% endapi-method-response-example-description %}

```
NFO qtp1564984895-14 - Loading plugin from: /usr/local/connector-plugins/rocketmq-replicator-0.1.0-SNAPSHOT.jar
2019-12-09 11:11:33 INFO qtp1564984895-14 - Loading plugin from: /usr/local/connector-plugins/rocketmq-connect-sample-0.0.1-SNAPSHOT.jar
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081/connectors" path="/:connector-name/config" %}
{% api-method-summary %}
获取某个connector的配置信息
{% endapi-method-summary %}

{% api-method-description %}
获取某个connector的配置信息
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="connector-name" type="string" required=true %}
connector名称
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
获取某个connector的配置信息
{% endapi-method-response-example-description %}

```
ConnectorConfigs:{"properties":{"xxx"}}
TaskConfigs:[{"properties":{"xxx"}}]
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081/connectors" path="/stopAll" %}
{% api-method-summary %}
停止并删除所有的connector和所有配置信息
{% endapi-method-summary %}

{% api-method-description %}
停止并删除所有的connector和所有配置信息
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
删除成功
{% endapi-method-response-example-description %}

```
success
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081" path="/getConfigInfo" %}
{% api-method-summary %}
获取所有connector和task配置信息 
{% endapi-method-summary %}

{% api-method-description %}
获取所有的配置信息
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
获取到所有的配置信息
{% endapi-method-response-example-description %}

```
ConnectorConfigs:{"xxx":{xxx}}
TaskConfigs:{"xxx":[xxx]}
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081" path="/getClusterInfo" %}
{% api-method-summary %}
获取集群信息
{% endapi-method-summary %}

{% api-method-description %}
获取集群信息
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
返回集群的具体信息
{% endapi-method-response-example-description %}

```
["172.xx.2.1x@23953","172.xx.1x.x@53324"]
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="get" host="http://localhost:8081" path="/getAllocatedInfo" %}
{% api-method-summary %}
获取负载信息
{% endapi-method-summary %}

{% api-method-description %}
获取当前worker的负载信息
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}

{% endapi-method-response-example-description %}

```
working connectors:{xxx}
working tasks:[xxx]
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

