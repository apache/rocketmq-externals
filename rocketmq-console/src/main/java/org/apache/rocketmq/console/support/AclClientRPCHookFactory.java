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


package org.apache.rocketmq.console.support;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acl Helper.
 *
 * @author 莫那·鲁道
 * @date 2019-04-18-09:27
 */
public class AclClientRPCHookFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AclClientRPCHookFactory.class);

    private ConcurrentHashMap<String, AclClientRPCHook> cache = new ConcurrentHashMap<>();

    private AclClientRPCHookFactory(){}

    public static AclClientRPCHookFactory getInstance() {
        return AclClientRPCHookFactoryLazyHolder.INSTANCE;
    }

    private static class AclClientRPCHookFactoryLazyHolder {

        private static final AclClientRPCHookFactory INSTANCE = new AclClientRPCHookFactory();
    }


    public AclClientRPCHook createAclClientRPCHook(String accessKey, String secretKey) {

        if (accessKey != null && secretKey != null) {
            String key = accessKey + "&" + secretKey;
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
            AclClientRPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
            cache.putIfAbsent(key, rpcHook);
            return rpcHook;
        }
        LOG.info("accessKey or secretKey is null, ak={}, sk={}", accessKey, secretKey);
        return null;
    }


}
