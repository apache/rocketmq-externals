# rocketmq-connect-CLI Admin

与RocketMQ中的mqadmin类似，使用简洁的CLI命令实现增加，删除，查看connector等功能

在rocketmq-connect\rocketmq-connect-CLI目录下，运行`sh connectAdmin`

```bash
The most commonly used connectAdmin commands are:
   createConnector      Create and start a connector by connector's config
   stopConnector        Stop a specific connector by connector-name
   queryConnectorConfig Get configuration information for a connector
   queryConnectorStatus Get Status information for a connector
   stopAll              Stop and delete all Connectors and all configuration information
   reloadPlugins        Reload the Connector file under the plugin directory
   getConfigInfo        Get all configuration information
   getClusterInfo       Get cluster information
   getAllocatedInfo     Get the load information of the current worker

See 'connectAdmin help <command>' for more information on a specific command.
```

如上所示，其中列出了最常用的命令，并附有简短说明。要获取每个命令的详细手册，请使用`sh connectAdmin help <command>`。例如，命令`sh connectAdmin help stopConnector`将输出如下内容：

```bash
$ sh connectAdmin help stopConnector
usage: connectAdmin stopConnector -c <arg> [-h]
 -c,--connectorName <arg>   connector name
 -h,--help                  Print help
```

## getAllocatedConnectors&getAllocatedTasks

提供格式化输出当前connectors和tasks

| taskName       | connectorName       | status     | topic     | update-timestamp |
| -------------- | ------------------- | ---------- | --------- | ---------------- |
| JdbcSourceTask | jdbcConnectorSource | TERMINATED | jdbcTopic | 1597409102590    |
| FileSourceTask | fileConnectorSource | TERMINATED | fileTopic | 1597409110815    |
| FileSinkTask   | fileConnectorSink   | STOPPING   | fileTopic | 1597409204516    |

## createConnector

其中要说明的一点是，createConnector需要指定配置文件的路径

```bash
$ sh connectAdmin help createConnector
usage: connectAdmin createConnector -c <arg> [-h] -p <arg>
 -c,--connectorName <arg>   connector name
 -h,--help                  Print help
 -p,--path <arg>            Configuration file pat
```

所以在启动新的connector时，要用`-p`指定json配置文件的路径，例如

```bash
sh connectAdmin createConnector -c fileConnectorSource -p /root/shell/file-connector.json
```

配置文件格式参考具体的connector，这里给出file-connector的格式

```json
{
    "connector-class":"org.apache.rocketmq.connect.file.FileSourceConnector",
 	"topic":"fileTopic",
 	"filename":"/opt/source-file/source-file.txt",
 	"source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"
}
```

