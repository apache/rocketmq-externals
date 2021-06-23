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

package org.apache.connect.mongo.replicator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class ReplicaSetManager {

    private static final Pattern HOST_PATTERN = Pattern.compile("((([^=]+)[=])?(([^/]+)\\/))?(.+)");

    private static final String HOST_SEPARATOR = ";";

    private final Map<String, ReplicaSetConfig> replicaConfigByName = new HashMap<>();

    public ReplicaSetManager(Set<ReplicaSetConfig> replicaSetConfigs) {
        replicaSetConfigs.forEach(replicaSetConfig -> {
            if (StringUtils.isNotBlank(replicaSetConfig.getReplicaSetName())) {
                replicaConfigByName.put(replicaSetConfig.getReplicaSetName(), replicaSetConfig);
            }
        });

        validate();
    }

    public static ReplicaSetManager create(String hosts) {
        Set<ReplicaSetConfig> replicaSetConfigs = new HashSet<>();
        if (hosts != null) {
            for (String replicaSetStr : StringUtils.split(hosts.trim(), HOST_SEPARATOR)) {
                if (StringUtils.isNotBlank(replicaSetStr)) {
                    ReplicaSetConfig replicaSetConfig = parseReplicaSetStr(replicaSetStr);
                    if (replicaSetConfig != null) {
                        replicaSetConfigs.add(replicaSetConfig);
                    }
                }
            }
        }
        return new ReplicaSetManager(replicaSetConfigs);
    }

    private static ReplicaSetConfig parseReplicaSetStr(String hosts) {
        if (hosts != null) {
            Matcher matcher = HOST_PATTERN.matcher(hosts);
            if (matcher.matches()) {
                String shard = matcher.group(3);
                String replicaSetName = matcher.group(5);
                String host = matcher.group(6);
                if (host != null && host.trim().length() != 0) {
                    return new ReplicaSetConfig(shard, replicaSetName, host);
                }
            }
        }
        return null;
    }

    private void validate() {
        Validate.isTrue(replicaConfigByName.size() > 0, "task config mongoAddr need special replicaSet addr");

    }

    public Map<String, ReplicaSetConfig> getReplicaConfigByName() {
        return replicaConfigByName;
    }
}
