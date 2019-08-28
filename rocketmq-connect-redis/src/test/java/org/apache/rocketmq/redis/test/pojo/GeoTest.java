package org.apache.rocketmq.redis.test.pojo;

import org.apache.rocketmq.connect.redis.pojo.Geo;
import org.junit.Assert;
import org.junit.Test;

public class GeoTest {
    @Test
    public void test(){
        Geo geo = new Geo();
        geo.setLatitude(1L);
        geo.setLongitude(2L);
        geo.setMember("hello");

        Assert.assertTrue(1L == geo.getLatitude());
        Assert.assertTrue(2L == geo.getLongitude());
        Assert.assertEquals("hello", geo.getMember());
    }
}
