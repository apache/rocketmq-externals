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
package org.apache.rocketmq.console.service.client;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExtImpl;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.joor.Reflect;

public class MQAdminInstance {
    private static final ThreadLocal<DefaultMQAdminExt> MQ_ADMIN_EXT_THREAD_LOCAL = new ThreadLocal<DefaultMQAdminExt>();
    private static final ThreadLocal<Integer> INIT_COUNTER = new ThreadLocal<Integer>();

    public static MQAdminExt threadLocalMQAdminExt() {
        DefaultMQAdminExt defaultMQAdminExt = MQ_ADMIN_EXT_THREAD_LOCAL.get();
        if (defaultMQAdminExt == null) {
            throw new IllegalStateException("defaultMQAdminExt should be init before you get this");
        }
        return defaultMQAdminExt;
    }

    public static RemotingClient threadLocalRemotingClient() {
        MQClientInstance mqClientInstance = threadLocalMqClientInstance();
        MQClientAPIImpl mQClientAPIImpl = Reflect.on(mqClientInstance).get("mQClientAPIImpl");
        return Reflect.on(mQClientAPIImpl).get("remotingClient");
    }

    public static MQClientInstance threadLocalMqClientInstance() {
        DefaultMQAdminExtImpl defaultMQAdminExtImpl = Reflect.on(MQAdminInstance.threadLocalMQAdminExt()).get("defaultMQAdminExtImpl");
        return Reflect.on(defaultMQAdminExtImpl).get("mqClientInstance");
    }

    public static void initMQAdminInstance(long timeoutMillis) throws MQClientException {
        Integer nowCount = INIT_COUNTER.get();
        if (nowCount == null) {
            DefaultMQAdminExt defaultMQAdminExt;
            if (timeoutMillis > 0) {
                defaultMQAdminExt = new DefaultMQAdminExt(timeoutMillis);
            }
            else {
                defaultMQAdminExt = new DefaultMQAdminExt();
            }
            defaultMQAdminExt.setInstanceName(Long.toString(System.currentTimeMillis()));
            defaultMQAdminExt.start();
            MQ_ADMIN_EXT_THREAD_LOCAL.set(defaultMQAdminExt);
            INIT_COUNTER.set(1);
        }
        else {
            INIT_COUNTER.set(nowCount + 1);
        }

    }

    public static void destroyMQAdminInstance() {
        Integer nowCount = INIT_COUNTER.get() - 1;
        if (nowCount > 0) {
            INIT_COUNTER.set(nowCount);
            return;
        }
        MQAdminExt mqAdminExt = MQ_ADMIN_EXT_THREAD_LOCAL.get();
        if (mqAdminExt != null) {
            mqAdminExt.shutdown();
            MQ_ADMIN_EXT_THREAD_LOCAL.remove();
            INIT_COUNTER.remove();
        }
    }
}
