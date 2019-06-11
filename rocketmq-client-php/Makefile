
#Licensed to the Apache Software Foundation (ASF) under one or more
#contributor license agreements.  See the NOTICE file distributed with
#this work for additional information regarding copyright ownership.
#The ASF licenses this file to You under the Apache License, Version 2.0
#(the "License"); you may not use this file except in compliance with
#the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



CPP             = g++
RM              = rm -f
CPP_FLAGS       = -Wall -c -I. -O2 -std=c++11

LIBRARY_DIR		= `php-config --extension-dir` 

LD              = g++
LD_FLAGS        = -Wall -shared -O2
RESULT          = rocketmq.so

DIR_SRC         = ./src

SOURCES			= $(wildcard ${DIR_SRC}/*.cc)
OBJECTS         = $(SOURCES:%.cc=%.o)

all:	${OBJECTS} ${RESULT}

${RESULT}: ${OBJECTS}
		${LD} ${LD_FLAGS} -o $@ ${OBJECTS} -lphpcpp -lrocketmq  -lz -lcurl -lpthread


${OBJECTS}: 
		${CPP} ${CPP_FLAGS} -fpic -o $@ ${@:%.o=%.cc}

install:
		cp -f ${RESULT} ${LIBRARY_DIR}

clean:
		${RM} *.obj *~* ${OBJECTS} ${RESULT}
