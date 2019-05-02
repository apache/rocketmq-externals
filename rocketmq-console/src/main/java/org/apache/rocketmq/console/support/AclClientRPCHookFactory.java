package org.apache.rocketmq.console.support;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;

/**
 * Acl Helper.
 *
 * @author 莫那·鲁道
 * @date 2019-04-18-09:27
 */
public class AclClientRPCHookFactory {

    private ConcurrentHashMap<String, AclClientRPCHook> cache = new ConcurrentHashMap<>();

    private AclClientRPCHookFactory(){}

    public static AclClientRPCHookFactory getInstance() {
        return AclClientRPCHookFactoryLazyHolder.INSTANCE;
    }

    private static class AclClientRPCHookFactoryLazyHolder {

        private static final AclClientRPCHookFactory INSTANCE = new AclClientRPCHookFactory();
    }


    public AclClientRPCHook createAclClientRPCHook(String accessKey, String secretKey) {

        if (!StringUtils.isEmpty(accessKey) && !StringUtils.isEmpty(secretKey)) {
            String key = accessKey + "&" + secretKey;
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
            AclClientRPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
            cache.putIfAbsent(key, rpcHook);
            return rpcHook;
        }
        throw new IllegalArgumentException("If you have Acl enabled, then accessKey nor secretKey can not be null or empty.");
    }


}
