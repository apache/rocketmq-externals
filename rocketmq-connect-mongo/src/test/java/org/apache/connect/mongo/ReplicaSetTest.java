package org.apache.connect.mongo;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.connect.mongo.replicator.ReplicaSet;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReplicaSetTest {

    private ReplicaSet replicaSet;

    private SourceTaskConfig sourceTaskConfig;

    private ReplicaSetConfig replicaSetConfig;

    private ReplicaSetsContext replicaSetsContext;

    @Before
    public void before() {
        this.sourceTaskConfig = new SourceTaskConfig();
        this.replicaSetConfig = new ReplicaSetConfig("shardName1", "", "127.0.0.1:27027");
        this.replicaSetsContext = new ReplicaSetsContext(sourceTaskConfig);
        this.replicaSet = new ReplicaSet(replicaSetConfig, replicaSetsContext);
    }

    @Test
    public void testStartAndShutDown() throws NoSuchFieldException, IllegalAccessException {
        replicaSet.start();
        Field field = ReplicaSet.class.getDeclaredField("running");
        field.setAccessible(true);
        AtomicBoolean o = (AtomicBoolean) field.get(replicaSet);
        Assert.assertTrue(o.get());
        replicaSet.shutdown();
        Assert.assertFalse(o.get());
    }

    @Test
    public void testPause() throws Exception {
        replicaSet.pause();
        Field field = ReplicaSet.class.getDeclaredField("pause");
        field.setAccessible(true);
        boolean pause = (boolean) field.get(replicaSet);
        Assert.assertTrue(pause);
    }

    @Test
    public void testResume() throws Exception {
        replicaSet.resume();
        Field field = ReplicaSet.class.getDeclaredField("pause");
        field.setAccessible(true);
        boolean pause = (boolean) field.get(replicaSet);
        Assert.assertFalse(pause);
    }

}
