# Logstash RocketMQ Input Plugin

This is a plugin for [Logstash](https://github.com/elastic/logstash). It helps you send events to Apache RocketMQ in
Logstash.

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

## How to Use

### 1. Build the Plugin

#### 1.1 Prerequisite

- Generate the logstash-core jar file

  Because the plugin API is currently part of the Logstash codebase and not published to maven central repository, you
  must have a local copy of that available. You can obtain a copy of the Logstash codebase with the following git
  command:

```shell
git clone --branch <branch_name> --single-branch https://github.com/elastic/logstash.git <target_folder>
```

The `branch_name` should correspond to the version of Logstash containing the preferred revision of the Java plugin API.

- Generate the logstash-core jar file

After you have obtained a copy of the appropriate revision of the Logstash codebase, you need to compile it to generate
the `.jar` file containing the Java plugin API. From the root directory of your Logstash codebase (`$LS_HOME`), you can
compile it with `./gradlew assemble` (or `gradlew.bat assemble` if you’re running on Windows). This should produce
the `$LS_HOME/logstash-core/build/libs/logstash-core-x.y.z.jar` where `x`, `y`, and `z` refer to the version of
Logstash.

After you have successfully compiled Logstash, you need to tell your Java plugin where to find
the `logstash-core-x.y.z.jar file`. Create a new file named `gradle.properties` in the root folder of your plugin
project. That file should have a single line:

```
LOGSTASH_CORE_PATH=<target_folder>/logstash-core
```

where `target_folder` is the root folder of your local copy of the Logstash codebase.

#### 1.2 Package logstash-rocketmq-input plugin

First clone this plugin codebase to your local repository. Then run the Gradle packaging task with the following
command:

```shell
./gradlew gem
```

For Windows platforms: Substitute `gradlew.bat` for `./gradlew` as appropriate in the command.

That task will produce a `gem` file in the root directory of your plugin’s codebase with the
name `logstash-input-rocketmq-<version>.gem`.

### 2. Running the Plugin in Logstash

#### 2.1 Installing the Java plugin in Logstash

After you have packaged your Java plugin as a Ruby gem in **1.2**, you can install it in Logstash with this command:

```sh
bin/logstash-plugin install --no-verify --local /path/to/logstash-input-rocketmq-<version>.gem
```

For Windows platforms: Substitute backslashes for forward slashes as appropriate in the command.

#### 2.2 Running Logstash with RocketMQ input plugin

The following is a minimal Logstash configuration that can be used to test that the RocketMQ input plugin is correctly
installed and functioning.

```yaml
input {
  rocketmq {
  namesrv_addr => "localhost:9876"
  topic => "topic-test"
  group => "GID_input"
  }
}

output {
  stdout { }
}
```

As you can see above, the only required configuration options are `namesrv_addr`, `topic`, and `group`, which denotes
the name server address of Apache RocketMQ and the topic where you want to publish the event, respectively. For more
details, please reference the next section.

Copy the above Logstash configuration to a file such as `rocketmq_input.conf`. Logstash should then be started with:

```shell
bin/logstash -f /path/to/rocketmq_input.conf
```

## Configuration Options

|      Setting       |                                            Input type                                            | Required |                         Default                          | Description                                                                                                                                                               |
|:------------------:|:------------------------------------------------------------------------------------------------:|:--------:|:--------------------------------------------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    namesrv_addr    |                                              string                                              |   Yes    |                                                          | The address of name server                                                                                                                                                |
|       topic        |                                              string                                              |   Yes    |                                                          | The topic to consume messages from                                                                                                                                        |
|       group        |                                              string                                              |   Yes    |                                                          | The subscription group                                                                                                                                                    |
|   sub_expression   |                                              string                                              |    No    |                           "*"                            | Sub-expression for consuming                                                                                                                                              |
|  consumer_threads  |                                              number                                              |    No    | `max(Runtime.getRuntime().availableProcessors() / 2, 1)` | The number of consume threads                                                                                                                                             |
| consume_from_where | string, one of `CONSUME_FROM_LAST_OFFSET`, `CONSUME_FROM_FIRST_OFFSET`, `CONSUME_FROM_TIMESTAMP` |    No    |                `CONSUME_FROM_LAST_OFFSET`                | Where to consume message for the first time                                                                                                                               |
| consume_timestamp  |                                              string                                              |    No    |                           `""`                           | Timestamp of the beginning message to be consumed. Only valid when `consume_from_where` is set to `CONSUME_FROM_TIMESTAMP`                                                |
|   message_model    |                            codec, one of `CLUSTERING`, `BROADCASTING`                            |    No    |                       `CLUSTERING`                       | The message consume model                                                                                                                                                 |
|       codec        |                                  codec, one of `plain`, `json`                                   |    No    |                         `plain`                          | The codec used for input message body.  You can reference [logstash codec plugins](https://www.elastic.co/guide/en/logstash/current/codec-plugins.html) for more details. |

## Compatibility

This plugin has been tested on Logstash 8.0 with OpenJDK 8 and 11.

## Contributing

All contributions are welcome: ideas, patches, documentation, bug reports, complaints, and even something you drew up on
a napkin.

Programming is not a required skill. Whatever you've seen about open source and maintainers or community members
saying "send patches or die" - you will not see that here.

It is more important to the community that you are able to contribute.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
