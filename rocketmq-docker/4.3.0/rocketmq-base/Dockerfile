#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This is maintained as the base image for namesrv and broker.
#FROM java:8
FROM centos:7

RUN yum install -y java-1.8.0-openjdk-headless unzip gettext nmap-ncat openssl\
 && yum clean all -y

ARG version

# Rocketmq version
ENV ROCKETMQ_VERSION ${version}

# Rocketmq home
ENV ROCKETMQ_HOME  /opt/rocketmq-${ROCKETMQ_VERSION}
#ENV ROCKETMQ_HOME /opt/rocketmq-all-${ROCKETMQ_VERSION}-bin-release

WORKDIR  ${ROCKETMQ_HOME}

RUN mkdir -p \
 /opt/logs \
 /opt/store

# get the version
RUN curl https://dist.apache.org/repos/dist/release/rocketmq/${ROCKETMQ_VERSION}/rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip -o rocketmq.zip \
 && unzip rocketmq.zip \
 && mv rocketmq-all*/* . \
 && rmdir rocketmq-all*  \
 && rm rocketmq.zip

# add scripts
COPY scripts/ ${ROCKETMQ_HOME}/bin/

VOLUME /opt/logs /opt/store