package org.apache.connect.mongo;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.PositionStorageReader;
import io.openmessaging.connector.api.source.SourceTask;
import io.openmessaging.connector.api.source.SourceTaskContext;
import io.openmessaging.internal.DefaultKeyValue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.connector.MongoSourceTask;
import org.apache.connect.mongo.replicator.ReplicaSet;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.junit.Assert;
import org.junit.Test;

public class MongoSourceTaskTest {

    @Test
    public void testEmptyContextStart() throws NoSuchFieldException, IllegalAccessException {
        MongoSourceTask mongoSourceTask = new MongoSourceTask();
        DefaultKeyValue defaultKeyValue = new DefaultKeyValue();
        defaultKeyValue.put("mongoAddr", "test/127.0.0.1:27027");
        defaultKeyValue.put("positionTimeStamp", "11111111");
        defaultKeyValue.put("positionInc", "111");
        defaultKeyValue.put("serverSelectionTimeoutMS", "10");
        defaultKeyValue.put("dataSync", "true");

        Field context = SourceTask.class.getDeclaredField("context");
        context.setAccessible(true);
        context.set(mongoSourceTask, emptyTaskContext());
        mongoSourceTask.start(defaultKeyValue);

        Field replicaSetsContext = MongoSourceTask.class.getDeclaredField("replicaSetsContext");
        replicaSetsContext.setAccessible(true);
        ReplicaSetsContext setsContext = (ReplicaSetsContext) replicaSetsContext.get(mongoSourceTask);

        Field replicaSets = ReplicaSetsContext.class.getDeclaredField("replicaSets");
        replicaSets.setAccessible(true);
        List<ReplicaSet> replicaSetList = (List<ReplicaSet>) replicaSets.get(setsContext);
        Assert.assertTrue(replicaSetList.size() == 1);
        ReplicaSet replicaSet = replicaSetList.get(0);
        Field replicaSetConfig = ReplicaSet.class.getDeclaredField("replicaSetConfig");
        replicaSetConfig.setAccessible(true);
        ReplicaSetConfig replicaSetConfig1 = (ReplicaSetConfig) replicaSetConfig.get(replicaSet);
        Assert.assertTrue(StringUtils.equals(replicaSetConfig1.getReplicaSetName(), "test"));
        Assert.assertTrue(StringUtils.equals(replicaSetConfig1.getHost(), "127.0.0.1:27027"));
        Assert.assertTrue(replicaSetConfig1.getPosition().getTimeStamp() == 11111111);
        Assert.assertTrue(replicaSetConfig1.getPosition().getInc() == 111);
        Assert.assertTrue(replicaSetConfig1.getPosition().isInitSync());
    }

    private SourceTaskContext emptyTaskContext() {
        return new SourceTaskContext() {
            @Override
            public PositionStorageReader positionStorageReader() {
                return new PositionStorageReader() {
                    @Override
                    public ByteBuffer getPosition(ByteBuffer partition) {
                        return null;
                    }

                    @Override
                    public Map<ByteBuffer, ByteBuffer> getPositions(Collection<ByteBuffer> partitions) {
                        return null;
                    }
                };
            }

            @Override
            public KeyValue configs() {
                return null;
            }
        };
    }

    @Test
    public void testContextStart() throws NoSuchFieldException, IllegalAccessException {
        MongoSourceTask mongoSourceTask = new MongoSourceTask();
        DefaultKeyValue defaultKeyValue = new DefaultKeyValue();
        defaultKeyValue.put("mongoAddr", "test/127.0.0.1:27027");
        defaultKeyValue.put("serverSelectionTimeoutMS", "10");

        Field context = SourceTask.class.getDeclaredField("context");
        context.setAccessible(true);
        context.set(mongoSourceTask, TaskContext());
        mongoSourceTask.start(defaultKeyValue);

        Field replicaSetsContext = MongoSourceTask.class.getDeclaredField("replicaSetsContext");
        replicaSetsContext.setAccessible(true);
        ReplicaSetsContext setsContext = (ReplicaSetsContext) replicaSetsContext.get(mongoSourceTask);

        Field replicaSets = ReplicaSetsContext.class.getDeclaredField("replicaSets");
        replicaSets.setAccessible(true);
        List<ReplicaSet> replicaSetList = (List<ReplicaSet>) replicaSets.get(setsContext);
        Assert.assertTrue(replicaSetList.size() == 1);
        ReplicaSet replicaSet = replicaSetList.get(0);
        Field replicaSetConfig = ReplicaSet.class.getDeclaredField("replicaSetConfig");
        replicaSetConfig.setAccessible(true);
        ReplicaSetConfig replicaSetConfig1 = (ReplicaSetConfig) replicaSetConfig.get(replicaSet);
        Assert.assertTrue(StringUtils.equals(replicaSetConfig1.getReplicaSetName(), "test"));
        Assert.assertTrue(StringUtils.equals(replicaSetConfig1.getHost(), "127.0.0.1:27027"));
        Assert.assertTrue(replicaSetConfig1.getPosition().getTimeStamp() == 22222222);
        Assert.assertTrue(replicaSetConfig1.getPosition().getInc() == 222);
        Assert.assertTrue(!replicaSetConfig1.getPosition().isInitSync());
    }

    private SourceTaskContext TaskContext() {
        return new SourceTaskContext() {
            @Override
            public PositionStorageReader positionStorageReader() {
                return new PositionStorageReader() {
                    @Override
                    public ByteBuffer getPosition(ByteBuffer partition) {

                        Map<String, Object> po = new HashMap<>();
                        po.put("timeStamp", 22222222);
                        po.put("inc", 222);
                        po.put("initSync", false);
                        return ByteBuffer.wrap(JSONObject.toJSONString(po).getBytes());
                    }

                    @Override
                    public Map<ByteBuffer, ByteBuffer> getPositions(Collection<ByteBuffer> partitions) {
                        return null;
                    }
                };
            }

            @Override
            public KeyValue configs() {
                return null;
            }
        };
    }
}
