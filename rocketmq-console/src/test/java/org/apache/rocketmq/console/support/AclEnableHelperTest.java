package org.apache.rocketmq.console.support;

import org.apache.rocketmq.console.config.RMQConfigure;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author 莫那·鲁道
 * @date 2019-05-02-21:40
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class AclEnableHelperTest {


    @Autowired
    AclEnableHelper aclEnableHelper;

    @Autowired
    RMQConfigure rmqConfigure;

    @Test
    public void isAclEnable() {

        rmqConfigure.setAclEnable("false");

        boolean b = aclEnableHelper.isAclEnable();
        Assert.assertEquals(b, false);

        rmqConfigure.setAclEnable("true");

        boolean b2 = aclEnableHelper.isAclEnable();
        Assert.assertEquals(b2, true);
    }
}