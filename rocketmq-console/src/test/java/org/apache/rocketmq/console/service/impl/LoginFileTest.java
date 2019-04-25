package org.apache.rocketmq.console.service.impl;

import org.apache.rocketmq.console.config.RMQConfigure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoginFileTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoad() throws Exception {
        RMQConfigure configure = new RMQConfigure();
        configure.setDataPath(this.getClass().getResource("/").getPath());

        UserServiceImpl.FileBasedUserInfoStore fileBasedUserInfoStore = new UserServiceImpl.FileBasedUserInfoStore(configure);
        Assert.assertTrue("No exception raise for FileBasedUserInfoStore", true);
    }
}
