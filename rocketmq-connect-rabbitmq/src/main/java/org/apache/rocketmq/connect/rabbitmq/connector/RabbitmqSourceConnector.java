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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.connect.rabbitmq.connector;

import io.openmessaging.connector.api.Task;
import java.util.Set;
import org.apache.rocketmq.connect.jms.connector.BaseJmsSourceConnector;
import org.apache.rocketmq.connect.rabbitmq.RabbitmqConfig;

public class RabbitmqSourceConnector extends BaseJmsSourceConnector {

    @Override
    public Class<? extends Task> taskClass() {
        return RabbitmqSourceTask.class;
    }

    public Set<String> getRequiredConfig() {
        return RabbitmqConfig.REQUEST_CONFIG;
    }
}
