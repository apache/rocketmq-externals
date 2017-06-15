/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Throwables;
import javax.annotation.Resource;
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.console.service.ProducerService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.springframework.stereotype.Service;

@Service
public class ProducerServiceImpl implements ProducerService {
    @Resource
    private MQAdminExt mqAdminExt;

    @Override
    public ProducerConnection getProducerConnection(String producerGroup, String topic) {
        try {
            return mqAdminExt.examineProducerConnectionInfo(producerGroup, topic);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
