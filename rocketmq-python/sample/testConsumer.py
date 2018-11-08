#/*
#* Licensed to the Apache Software Foundation (ASF) under one or more
#* contributor license agreements.  See the NOTICE file distributed with
#* this work for additional information regarding copyright ownership.
#* The ASF licenses this file to You under the Apache License, Version 2.0
#* (the "License"); you may not use this file except in compliance with
#* the License.  You may obtain a copy of the License at
#*
#*     http://www.apache.org/licenses/LICENSE-2.0
#*
#* Unless required by applicable law or agreed to in writing, software
#* distributed under the License is distributed on an "AS IS" BASIS,
#* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#* See the License for the specific language governing permissions and
#* limitations under the License.
#*/

import base
import time
from librocketmqclientpython import *
totalMsg = 0
def consumerMessage(msg):
     global totalMsg
     totalMsg += 1
     print(">>ConsumerMessage Called:",totalMsg)
     print(GetMessageTopic(msg))
     print(GetMessageTags(msg))
     print(GetMessageBody(msg))
     print(GetMessageId(msg))
     return 0

print("Consumer Starting.....")

consumer = CreatePushConsumer("awtTest_Producer_Python_Test")
print(consumer)
SetPushConsumerNameServerAddress(consumer,"172.17.0.5:9876")
SetPushConsumerThreadCount(consumer,1)
Subscribe(consumer, "T_TestTopic", "*")
RegisterMessageCallback(consumer,consumerMessage)
StartPushConsumer(consumer)
i = 1
while i <= 60:
    print(i)
    i += 1
    time.sleep(10) 

ShutdownPushConsumer(consumer)
DestroyPushConsumer(consumer)
print("Consumer Down....")
