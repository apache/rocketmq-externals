package org.apache.rocketmq.console.support;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.junit.Assert;
import org.junit.Test;


public class AclClientRPCHookFactoryTest {

    @Test
    public void createAclClientRPCHook() {

        AclClientRPCHook hook =
            AclClientRPCHookFactory.getInstance().createAclClientRPCHook("1234567", "1234567");

        Assert.assertNotNull(hook);

        try {

            AclClientRPCHook hook2 =
                AclClientRPCHookFactory.getInstance().createAclClientRPCHook("", "");
        } catch (Exception e) {
            Assert.assertEquals(e.getClass(), IllegalArgumentException.class);
        }

        try {
            AclClientRPCHook hook3 =
                AclClientRPCHookFactory.getInstance().createAclClientRPCHook(null, null);
            Assert.assertEquals(hook3, null);
        } catch (Exception e) {
            Assert.assertEquals(e.getClass(), IllegalArgumentException.class);
        }
    }
}