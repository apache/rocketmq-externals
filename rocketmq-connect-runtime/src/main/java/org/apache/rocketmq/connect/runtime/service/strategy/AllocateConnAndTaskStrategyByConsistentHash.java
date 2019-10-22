/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.connect.runtime.service.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.consistenthash.ConsistentHashRouter;
import org.apache.rocketmq.common.consistenthash.HashFunction;
import org.apache.rocketmq.common.consistenthash.Node;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocateConnAndTaskStrategyByConsistentHash implements AllocateConnAndTaskStrategy {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private int virtualNodes;
    private HashFunction hashFunc;

    public AllocateConnAndTaskStrategyByConsistentHash() {
        virtualNodes = Integer.parseInt(System.getProperty(RuntimeConfigDefine.VIRTUAL_NODE, "0"));
        String funcName = System.getProperty(RuntimeConfigDefine.HASH_FUNC);
        if (StringUtils.isNoneEmpty(funcName)) {
            try {
                hashFunc = (HashFunction) Class.forName(funcName).newInstance();
            } catch (Exception e) {
                log.error("custom hashFunc: {} failed", funcName, e);
                throw new IllegalArgumentException(String.format("init funcName: %s failed.", funcName));
            }
        }
    }

    @Override public ConnAndTaskConfigs allocate(List<String> allWorker, String curWorker,
        Map<String, ConnectKeyValue> connectorConfigs, Map<String, List<ConnectKeyValue>> taskConfigs) {
        ConnAndTaskConfigs allocateResult = new ConnAndTaskConfigs();
        if (null == allWorker || 0 == allWorker.size()) {
            return allocateResult;
        }

        Collection<ClientNode> cidNodes = allWorker.stream().map(ClientNode::new).collect(Collectors.toList());
        ConsistentHashRouter router = getRouter(cidNodes);

        connectorConfigs.entrySet().stream().filter(task -> curWorker.equals(router.routeNode(task.getKey()).getKey()))
            .forEach(task -> allocateResult.getConnectorConfigs().put(task.getKey(), task.getValue()));

        for (Map.Entry<String, List<ConnectKeyValue>> connector : taskConfigs.entrySet()) {
            connector.getValue().stream().filter(kv -> curWorker.equals(router.routeNode(kv.toString()).getKey()))
                .forEach(allocateResult.getTaskConfigs().computeIfAbsent(connector.getKey(), k -> new ArrayList<>())::add);
        }
        log.debug("allocate result: " + allocateResult);
        return allocateResult;
    }

    private ConsistentHashRouter getRouter(Collection<ClientNode> cidNodes) {
        int virtualNodeCnt = virtualNodes;
        if (virtualNodeCnt == 0) {
            virtualNodeCnt = cidNodes.size();
        }

        ConsistentHashRouter<ClientNode> router;
        if (hashFunc == null) {
            router = new ConsistentHashRouter<>(cidNodes, virtualNodeCnt);
        } else {
            router = new ConsistentHashRouter<>(cidNodes, virtualNodeCnt, hashFunc);
        }
        return router;
    }

    private static class ClientNode implements Node {
        private final String clientID;

        private ClientNode(String clientID) {
            this.clientID = clientID;
        }

        @Override
        public String getKey() {
            return clientID;
        }
    }
}
