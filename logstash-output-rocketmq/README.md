# Logstash Java Plugin

[![Travis Build Status](https://travis-ci.com/logstash-plugins/logstash-output-java_output_example.svg)](https://travis-ci.com/logstash-plugins/logstash-output-java_output_example)

This is a Java plugin for [Logstash](https://github.com/elastic/logstash).

This is a plug-in that outputs logstash logs to rocketmq. It supports synchronous, asynchronous, batch, oneway and other sending modes. You can configure the timeout, the number of retries, etc. If the message fails to be sent, an alarm email can be sent.

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

The documentation for Logstash Java plugins is available [here](https://www.elastic.co/guide/en/logstash/6.7/contributing-java-plugin.html).

Plugin installation steps
1. Configure LOGSTASH_CORE_PATH in gradle.properties
2. `gradlew.bat assemble` 
3. `gredlew gem`
4. Modify source in gemfile to `source  'https://gems.ruby-china.com'`
5. Enter the root directory of logstash, and install it with the command 
   ` bin\logstash-plugin install --no-verify --local logstash-output-rocketmq-1.0.1.gem`
6. Put the configuration file in the root directory of logstash, and test it with the command `bin/logstash -f test.conf.txt`
