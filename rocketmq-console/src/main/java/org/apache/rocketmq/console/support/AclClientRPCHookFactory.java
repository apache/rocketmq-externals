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
