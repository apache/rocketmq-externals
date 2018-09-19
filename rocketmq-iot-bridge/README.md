# IoT Bridge for Apache RocketMQ

IoT Bridge for Apache RocketMQ is a solution to reliable and real-time message service for IoT devices.

## Main features
- Multiple protocols support
    - MQTT
    - CoAP
- Pluggable message storage
- Pluggable subscription storage
- Pluggable upstream data processing extensions

## Get Started
### Build from source
1. Clone the code
```shell
git clone http://github.com/apache/rocketmq-externals
```

2. Build the binary
```shell
cd iot-bridge
mvn clean package -DskipTests=true
```

3. Try it
Start the server
```
sh bin/server.sh start
```
Start the Consumer
```
sh bin/sample-consumer.sh
```
Start the Producer
```
sh bin/sample-producer.sh
```

## Read More
See the [documents](docs/index.md).

## License

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

