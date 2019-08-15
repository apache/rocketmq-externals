package org.apache.connect.mongo.replicator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class ReplicaSets {

    private static final Pattern HOST_PATTERN = Pattern.compile("((([^=]+)[=])?(([^/]+)\\/))?(.+)");

    private final Map<String, ReplicaSetConfig> replicaConfigByName = new HashMap<>();

    public ReplicaSets(Set<ReplicaSetConfig> replicaSetConfigs) {
        replicaSetConfigs.forEach(replicaSetConfig -> {
            if (StringUtils.isNotBlank(replicaSetConfig.getReplicaSetName())) {
                replicaConfigByName.put(replicaSetConfig.getReplicaSetName(), replicaSetConfig);
            }
        });

        validate();
    }

    public static ReplicaSets create(String hosts) {
        Set<ReplicaSetConfig> replicaSetConfigs = new HashSet<>();
        if (hosts != null) {
            for (String replicaSetStr : StringUtils.split(hosts.trim(), ";")) {
                if (StringUtils.isNotBlank(replicaSetStr)) {
                    ReplicaSetConfig replicaSetConfig = parseReplicaSetStr(replicaSetStr);
                    if (replicaSetConfig != null) {
                        replicaSetConfigs.add(replicaSetConfig);
                    }
                }
            }
        }
        return new ReplicaSets(replicaSetConfigs);
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
        Validate.isTrue(replicaConfigByName.size() > 0, "task config mongoAdd need special replicaSet addr");

    }

    public Map<String, ReplicaSetConfig> getReplicaConfigByName() {
        return replicaConfigByName;
    }
}
