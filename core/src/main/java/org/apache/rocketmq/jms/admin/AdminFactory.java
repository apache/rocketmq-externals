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

package org.apache.rocketmq.jms.admin;

import java.util.concurrent.ConcurrentHashMap;
import javax.jms.JMSException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.jms.exception.JMSClientException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

public class AdminFactory {

    private static ConcurrentHashMap<String/*nameServerAddress*/, DefaultMQAdminExt> admins = new ConcurrentHashMap();

    public static DefaultMQAdminExt getAdmin(String nameServerAddress) throws JMSException {
        if (nameServerAddress == null) {
            throw new IllegalArgumentException("NameServerAddress could be null");
        }

        DefaultMQAdminExt admin = admins.get(nameServerAddress);
        if (admin != null) {
            return admin;
        }

        admin = new DefaultMQAdminExt(nameServerAddress);
        try {
            admin.start();
        }
        catch (MQClientException e) {
            throw new JMSClientException("Error during starting admin client");
        }
        DefaultMQAdminExt old = admins.putIfAbsent(nameServerAddress, admin);
        if (old != null) {
            admin.shutdown();
            return old;
        }

        return admin;
    }
}
