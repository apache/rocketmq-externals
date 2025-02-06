#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

begin
  require 'jar_dependencies'
rescue LoadError
  require 'io/netty/netty-tcnative-boringssl-static/1.1.33.Fork26/netty-tcnative-boringssl-static-1.1.33.Fork26.jar'
  require 'com/alibaba/fastjson/1.2.75/fastjson-1.2.75.jar'
  require 'org/apache/rocketmq/rocketmq-common/4.8.0/rocketmq-common-4.8.0.jar'
  require 'commons-beanutils/commons-beanutils/1.9.2/commons-beanutils-1.9.2.jar'
  require 'org/apache/rocketmq/rocketmq-logging/4.8.0/rocketmq-logging-4.8.0.jar'
  require 'commons-validator/commons-validator/1.6/commons-validator-1.6.jar'
  require 'commons-digester/commons-digester/1.8.1/commons-digester-1.8.1.jar'
  require 'commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar'
  require 'io/netty/netty-all/4.1.63.Final/netty-all-4.1.63.Final.jar'
  require 'org/apache/commons/commons-lang3/3.11/commons-lang3-3.11.jar'
  require 'commons-logging/commons-logging/1.2/commons-logging-1.2.jar'
  require 'org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar'
  require 'org/apache/rocketmq/rocketmq-remoting/4.8.0/rocketmq-remoting-4.8.0.jar'
  require 'org/apache/rocketmq/rocketmq-client/4.8.0/rocketmq-client-4.8.0.jar'
end

if defined? Jars
  require_jar 'io.netty', 'netty-tcnative-boringssl-static', '1.1.33.Fork26'
  require_jar 'com.alibaba', 'fastjson', '1.2.75'
  require_jar 'org.apache.rocketmq', 'rocketmq-common', '4.8.0'
  require_jar 'commons-beanutils', 'commons-beanutils', '1.9.2'
  require_jar 'org.apache.rocketmq', 'rocketmq-logging', '4.8.0'
  require_jar 'commons-validator', 'commons-validator', '1.6'
  require_jar 'commons-digester', 'commons-digester', '1.8.1'
  require_jar 'commons-collections', 'commons-collections', '3.2.2'
  require_jar 'io.netty', 'netty-all', '4.1.63.Final'
  require_jar 'org.apache.commons', 'commons-lang3', '3.11'
  require_jar 'commons-logging', 'commons-logging', '1.2'
  require_jar 'org.slf4j', 'slf4j-api', '1.7.25'
  require_jar 'org.apache.rocketmq', 'rocketmq-remoting', '4.8.0'
  require_jar 'org.apache.rocketmq', 'rocketmq-client', '4.8.0'
end
