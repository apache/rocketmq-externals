package org.apache.rocketmq.redis.test.common;

import org.apache.rocketmq.connect.redis.common.Options;
import org.junit.Assert;
import org.junit.Test;


public class OptionsTest {

    @Test
    public void test(){
        Options options1 = Options.valueOf("TEST");
        Assert.assertNull(options1);

        Options options2 = Options.REDIS_PARTITION;
        Assert.assertEquals(options2.name(), "DEFAULT_PARTITION");

        Assert.assertNotNull(options2.toString());

        Options options3 = Options.valueOf("DEFAULT_PARTITION");
        Assert.assertTrue(options3.equals(options2));
    }

    @Test
    public void testNull(){
        Exception ex = null;
        try {
            Options.valueOf("");
        }catch (Exception e){
            ex = e;
        }
        Assert.assertNotNull(ex);
    }
}
