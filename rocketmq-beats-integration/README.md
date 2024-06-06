# rocketmq-beats-integration

This project enables Apache RocketMQ integrate directly
with [Elastic Beats](https://www.elastic.co/guide/en/beats/libbeat/current/index.html).

## Compatibility

This project is developed and tested using Apache RocketMQ Go Client v2.1.0 and Beats 7.17.0

## Sending events to Apache RocketMQ in Beats Platform

### 1. Build your Beats

The Beats platform provides several Beats, like [Filebeat](https://www.elastic.co/products/beats/filebeat)
, [Heartbeat](https://www.elastic.co/products/beats/heartbeat)
, [Metricbeat](https://www.elastic.co/products/beats/metricbeat), etc. So you can choose to build one or more Beats
according to different sources you want to capture.

#### 1.1 Prerequisite

At present Beats Platform does not provide ways to integrate a custom output at runtime, so you must build Beats with
this codebase to generate a new Beat to support output to Apache RocketMQ.

You can obtain a copy of the Beat codebase with the following git command:

```shell
git clone --branch <branch_name> --single-branch https://github.com/elastic/beats.git <target_folder>
```

The `branch_name` should be the version of Beats you want to use. In the following `<target_folder>` is
denoted by `$BEATS_HOME`.

#### 1.2 Build specified Beats

**1.2.1** clone this codebase to your local repository, then copy the folder `rocketmq` under `libbeat/output`
to `$BEATS_HOME/libbeat/output/`.

**1.2.2** add `_ "github.com/elastic/beats/v7/libbeat/outputs/rocketmq"` in the `import` part
of `$BEATS_HOME/libbeat/publisher/includes/includes.go`:

```golang
import (
	// import queue types
	_ "github.com/elastic/beats/v7/libbeat/outputs/codec/format"
	_ "github.com/elastic/beats/v7/libbeat/outputs/codec/json"
	_ "github.com/elastic/beats/v7/libbeat/outputs/console"
	_ "github.com/elastic/beats/v7/libbeat/outputs/elasticsearch"
	_ "github.com/elastic/beats/v7/libbeat/outputs/fileout"
	_ "github.com/elastic/beats/v7/libbeat/outputs/kafka"
	_ "github.com/elastic/beats/v7/libbeat/outputs/logstash"
	_ "github.com/elastic/beats/v7/libbeat/outputs/redis"
	
	_ "github.com/elastic/beats/v7/libbeat/outputs/rocketmq"
	
	_ "github.com/elastic/beats/v7/libbeat/publisher/queue/diskqueue"
	_ "github.com/elastic/beats/v7/libbeat/publisher/queue/memqueue"
	_ "github.com/elastic/beats/v7/libbeat/publisher/queue/spool"
)
```

**1.2.3** run `go mod tidy` to import dependencies of RocketMQ go client, or add the following instruction
to `$BEATS_HOME/go.mode`:

```golang
require github.com/apache/rocketmq-client-go/v2 v2.1.0
```

**1.2.4** build your Beats

For example, if you want to build filebeat, run the following command:

```shell
cd $BEATS_HOME/filebeat/ && make
```

After that, a new executable `filebeat` is generated, and you can use it to send events to Apache RocketMQ.

If you want to build other Beats, just enter the specified folder and run `make`.

For example:

```shell
cd $BEATS_HOME/auditbeat/ && make
cd $BEATS_HOME/heartbeat/ && make
cd $BEATS_HOME/metricbeat/ && make
cd $BEATS_HOME/packetbeat/ && make
cd $BEATS_HOME/winlogbeat/ && make
```

### 2. Run your Beats

After you have built your Beats, you can run them to send events to Apache RocketMQ.

Here is a simple configuration that can be used to test that if the content of `/var/log/messages` would be sent to
RocketMQ successfully.

```yaml
filebeat.inputs:
  - type: filestream
    enabled: true
    paths:
      - /var/log/messages

output.rocketmq:
  nameservers: [ "127.0.0.1:9876" ]
  topic: TopicTest

```

As you can see above, the only required configuration options are `nameservers` and `topic`, which denotes the name
server address array of Apache RocketMQ and the topic where you want to publish the event, respectively. For more
details, please reference the next section.

Copy the above Filebeat configuration to a file such as `rocketmq_output.yml`. Filebeat should then be started with:

```shell
./filebeat -c rocketmq_output.yml -e
```

## Configuration Options

|     Setting     |               Input type               | Required |                       Default                       | Description                                                                                                                                                       |
|:---------------:|:--------------------------------------:|:--------:|:---------------------------------------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  nameservers   |                 []string                 |   Yes    |                                                     | The address list of name servers                                                                                                                                        |
|      topic      |                 string                 |   Yes    |                                                     | The topic to produce messages to                                                                                                                                  |
|      group      |                 string                 |    No    |                                      | The producer group                                                                                                                                                |
| send_timeout |                 time.Duration                 |    No    |                        3s                         | Send timeout milliseconds                                                                                                                                         |
|     max_retries     |                 number                 |    No    | 2 | Retry times after failing to send the event                                                                                                                       |
|      codec      |                 codec                  |    No    |                        plain                        | The codec used for output data. You can reference [output codec](https://www.elastic.co/guide/en/beats/filebeat/current/configuration-output-codec.html) for more details. |

## Contributing

All contributions are welcome: ideas, patches, documentation, bug reports, complaints, and even something you drew up on
a napkin.

Programming is not a required skill. Whatever you've seen about open source and maintainers or community members
saying "send patches or die" - you will not see that here.

It is more important to the community that you are able to contribute.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation