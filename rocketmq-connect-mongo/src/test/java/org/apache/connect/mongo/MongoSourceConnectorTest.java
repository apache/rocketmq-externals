package org.apache.connect.mongo;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.connect.mongo.connector.MongoSourceConnector;
import org.apache.connect.mongo.connector.MongoSourceTask;
import org.apache.connect.mongo.replicator.Position;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MongoSourceConnectorTest {

    private MongoSourceConnector mongoSourceConnector;
    private DefaultKeyValue keyValue;
    private SourceTaskConfig sourceTaskConfig;

    @Before
    public void before() {
        mongoSourceConnector = new MongoSourceConnector();
        keyValue = new DefaultKeyValue();
        sourceTaskConfig = new SourceTaskConfig();

    }

    @Test
    public void takeClass() {
        Assert.assertEquals(mongoSourceConnector.taskClass(), MongoSourceTask.class);
    }

    @Test
    public void verifyConfig() {
        String s = mongoSourceConnector.verifyAndSetConfig(keyValue);
        Assert.assertTrue(s.contains("Request config key:"));
    }

    @Test
    public void testPoll() throws Exception {
        LinkedBlockingQueue<SourceDataEntry> entries = new LinkedBlockingQueue<>();
        ReplicaSetsContext context = new ReplicaSetsContext(sourceTaskConfig);
        Field dataEntryQueue = ReplicaSetsContext.class.getDeclaredField("dataEntryQueue");
        dataEntryQueue.setAccessible(true);
        dataEntryQueue.set(context, entries);
        ReplicationEvent event = new ReplicationEvent();
        event.setOperationType(OperationType.INSERT);
        event.setNamespace("test.person");
        event.setTimestamp(new BsonTimestamp(1565609506, 1));
        event.setDocument(new Document("testKey", "testValue"));
        event.setH(324243242L);
        event.setEventData(Optional.ofNullable(new Document("testEventKey", "testEventValue")));
        event.setObjectId(Optional.empty());
        context.publishEvent(event, new ReplicaSetConfig("", "testReplicaName", "localhost:27027"));
        List<SourceDataEntry> sourceDataEntries = (List<SourceDataEntry>) context.poll();
        Assert.assertTrue(sourceDataEntries.size() == 1);

        SourceDataEntry sourceDataEntry = sourceDataEntries.get(0);
        Assert.assertEquals("test-person", sourceDataEntry.getQueueName());

        ByteBuffer sourcePartition = sourceDataEntry.getSourcePartition();
        Assert.assertEquals("testReplicaName", new String(sourcePartition.array()));

        ByteBuffer sourcePosition = sourceDataEntry.getSourcePosition();
        Position position = JSONObject.parseObject(new String(sourcePosition.array()), Position.class);
        Assert.assertEquals(position.getTimeStamp(), 1565609506);
        Assert.assertEquals(position.getInc(), 1);
        Assert.assertEquals(position.isInitSync(), false);

        EntryType entryType = sourceDataEntry.getEntryType();
        Assert.assertEquals(EntryType.CREATE, entryType);

        String queueName = sourceDataEntry.getQueueName();
        Assert.assertEquals("test-person", queueName);

        Schema schema = sourceDataEntry.getSchema();
        Assert.assertTrue(schema.getFields().size() == 6);
        Object[] payload = sourceDataEntry.getPayload();
        Assert.assertTrue(payload.length == 6);

    }

}
