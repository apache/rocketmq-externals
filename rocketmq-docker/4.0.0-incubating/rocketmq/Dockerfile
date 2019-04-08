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

FROM centos:7

RUN yum install -y java-1.8.0-openjdk-headless unzip gettext nmap-ncat openssl\
 && yum clean all -y

ARG version

# Rocketmq version
ENV ROCKETMQ_VERSION ${version}

# Rocketmq home
ENV ROCKETMQ_HOME  /opt/rocketmq-${ROCKETMQ_VERSION}

WORKDIR  ${ROCKETMQ_HOME}

# get the version
RUN curl https://dist.apache.org/repos/dist/release/rocketmq/${ROCKETMQ_VERSION}/rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip -o rocketmq.zip \
 && unzip rocketmq.zip \
 && mv apache-rocketmq-all*/* . \
 && rmdir apache-rocketmq-all*  \
 && rm rocketmq.zip

# add scripts
COPY scripts/ ${ROCKETMQ_HOME}/bin/

#VOLUME /opt/logs /opt/store

# expose namesrv port
EXPOSE 9876

# add customized scripts for namesrv
RUN mv ${ROCKETMQ_HOME}/bin/runserver-customize.sh ${ROCKETMQ_HOME}/bin/runserver.sh \
 && chmod +x ${ROCKETMQ_HOME}/bin/runserver.sh \
 && chmod +x ${ROCKETMQ_HOME}/bin/mqnamesrv \
 && chmod +x ${ROCKETMQ_HOME}/bin/mqadmin

# expose broker ports
EXPOSE 10909 10911

# add customized scripts for broker
RUN mv ${ROCKETMQ_HOME}/bin/runbroker-customize.sh ${ROCKETMQ_HOME}/bin/runbroker.sh \
 && chmod +x ${ROCKETMQ_HOME}/bin/runbroker.sh \
 && chmod +x ${ROCKETMQ_HOME}/bin/mqbroker \
 && chmod +x ${ROCKETMQ_HOME}/bin/mqadmin

# export Java options
RUN export JAVA_OPT=" -Duser.home=/opt"

ARG user=rocketmq
ARG group=rocketmq
ARG uid=3000
ARG gid=3000

# RocketMQ is run with user `rocketmq`, uid = 3000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -u ${uid} -g ${gid} -m -s /bin/bash ${user}

USER ${user}

WORKDIR ${ROCKETMQ_HOME}/bin