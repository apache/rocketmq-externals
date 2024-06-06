# Logstash RocketMQ Output Plugin

This is a plugin for [Logstash](https://github.com/elastic/logstash). It helps you send events to Apache RocketMQ in Logstash.

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

## How to Use
### 1. Build the Plugin
#### 1.1 Prerequisite
- Generate the logstash-core jar file

  Because the plugin API is currently part of the Logstash codebase and not published to maven central repository, you must have a local copy of that available. 
You can obtain a copy of the Logstash codebase with the following git command:
```shell
git clone --branch <branch_name> --single-branch https://github.com/elastic/logstash.git <target_folder>
```
The `branch_name` should correspond to the version of Logstash containing the preferred revision of the Java plugin API.

- Generate the logstash-core jar file

After you have obtained a copy of the appropriate revision of the Logstash codebase, you need to compile it to generate the `.jar` file containing the Java plugin API. 
From the root directory of your Logstash codebase (`$LS_HOME`), you can compile it with `./gradlew assemble` (or `gradlew.bat assemble` if you’re running on Windows). 
This should produce the `$LS_HOME/logstash-core/build/libs/logstash-core-x.y.z.jar` where `x`, `y`, and `z` refer to the version of Logstash.

After you have successfully compiled Logstash, you need to tell your Java plugin where to find the `logstash-core-x.y.z.jar file`. 
Create a new file named `gradle.properties` in the root folder of your plugin project. That file should have a single line:
```
LOGSTASH_CORE_PATH=<target_folder>/logstash-core
```
where `target_folder` is the root folder of your local copy of the Logstash codebase.


#### 1.2 Package logstash-rocketmq-output plugin
First clone this plugin codebase to your local repository.
Then run the Gradle packaging task with the following command:
```shell
./gradlew gem
```
For Windows platforms: Substitute `gradlew.bat` for `./gradlew` as appropriate in the command.

That task will produce a `gem` file in the root directory of your plugin’s codebase with the name `logstash-output-rocketmq-<version>.gem`.



### 2. Running the Plugin in Logstash
#### 2.1 Installing the Java plugin in Logstash
After you have packaged your Java plugin as a Ruby gem in **1.2**, you can install it in Logstash with this command:

```sh
bin/logstash-plugin install --no-verify --local /path/to/logstash-output-rocketmq-<version>.gem
```
For Windows platforms: Substitute backslashes for forward slashes as appropriate in the command.

#### 2.2 Running Logstash with RocketMQ output plugin
The following is a minimal Logstash configuration that can be used to test that the RocketMQ output plugin is correctly installed and functioning.
```yaml
input {
  generator { message => "Hello RocketMQ!" count => 10 }
}
output {
  rocketmq {
    namesrv_addr => "localhost:9876"
    topic => "topic-test"
  }
}
```
As you can see above, the only required configuration options are `namesrv_addr` and `topic`, which denotes the name server address of 
Apache RocketMQ and the topic where you want to publish the event, respectively. For more details, please reference the next section.

Copy the above Logstash configuration to a file such as `rocketmq_output.conf`. Logstash should then be started with:
```shell
bin/logstash -f /path/to/rocketmq_output.conf
```

## Configuration Options
|     Setting     |               Input type               | Required |                       Default                       | Description                                                                                                                                                       |
|:---------------:|:--------------------------------------:|:--------:|:---------------------------------------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  namesrv_addr   |                 string                 |   Yes    |                                                     | The address of name server                                                                                                                                        |
|      topic      |                 string                 |   Yes    |                                                     | The topic to produce messages to                                                                                                                                  |
|      group      |                 string                 |    No    |                   LOGSTASH_GROUP                    | The producer group                                                                                                                                                |
|       tag       |                 string                 |    No    |                        `""`                         | The tag of the event                                                                                                                                              |
|    send_mode    | string, one of `ONEWAY`,`ASYNC`,`SYNC` |    No    |                       ONEWAY                        | Send mode for the event                                                                                                                                           |
| send_timeout_ms |                 number                 |    No    |                        3000                         | Send timeout milliseconds                                                                                                                                         |
|     retries     |                 number                 |    No    | Use default retry times of Apache RocketMQ producer | Retry times after failing to send the event                                                                                                                       |
|      codec      |                 codec                  |    No    |                        plain                        | The codec used for output data. You can reference [logstash codec plugins](https://www.elastic.co/guide/en/logstash/current/codec-plugins.html) for more details. |


## Compatibility
This plugin has been tested on Logstash 8.0 with OpenJDK 8 and 11.

## Contributing
All contributions are welcome: ideas, patches, documentation, bug reports, complaints, and even something you drew up on a napkin.

Programming is not a required skill. Whatever you've seen about open source and maintainers or community members saying "send patches or die" - you will not see that here.

It is more important to the community that you are able to contribute.

## License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
