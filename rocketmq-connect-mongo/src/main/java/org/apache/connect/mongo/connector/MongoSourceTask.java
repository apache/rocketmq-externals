package org.apache.connect.mongo.connector;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.*;
import io.openmessaging.connector.api.source.SourceTask;
import org.apache.connect.mongo.replicator.Constants;
import org.apache.connect.mongo.MongoReplicatorConfig;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.apache.connect.mongo.replicator.MongoReplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MongoSourceTask extends SourceTask {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MongoReplicator mongoReplicator;

    private MongoReplicatorConfig replicatorConfig;

    private String mongoSource;

    @Override
    public Collection<SourceDataEntry> poll() {
        List<SourceDataEntry> res = new ArrayList<>();
        ReplicationEvent event = mongoReplicator.getQueue().poll();
        if (event == null) {
            return new ArrayList<>();
        }

        JSONObject position = position(event);
        Schema schema = new Schema();
        schema.setDataSource(event.getDatabaseName());
        schema.setName(event.getCollectionName());
        schema.setFields(new ArrayList<>());
        buildFieleds(schema);
        DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
        dataEntryBuilder.timestamp(System.currentTimeMillis())
                .queue(event.getNamespace().replace(".", "-"))
                .entryType(event.getEntryType());

        if (event.getOperationType().ordinal() == OperationType.CREATED.ordinal()) {
            dataEntryBuilder.putFiled(Constants.CREATED, event.getDocument().toJson());
            dataEntryBuilder.putFiled(Constants.NAMESPACE, event.getNamespace());
        } else {
            dataEntryBuilder.putFiled(Constants.OPERATIONTYPE, event.getOperationType().name());
            dataEntryBuilder.putFiled(Constants.TIMESTAMP, event.getTimestamp().getValue());
            dataEntryBuilder.putFiled(Constants.VERSION, event.getV());
            dataEntryBuilder.putFiled(Constants.NAMESPACE, event.getNamespace());
            dataEntryBuilder.putFiled(Constants.PATCH, event.getEventData().isPresent() ? JSONObject.toJSONString(event.getEventData().get()) : "");
            dataEntryBuilder.putFiled(Constants.OBJECTID, event.getObjectId().isPresent() ? JSONObject.toJSONString(event.getObjectId().get()) : "");
        }
        SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                ByteBuffer.wrap(mongoSource.getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap(position.toJSONString().getBytes(StandardCharsets.UTF_8)));
        res.add(sourceDataEntry);
        return res;
    }

    @Override
    public void start(KeyValue config) {
        try {
            replicatorConfig = new MongoReplicatorConfig();
            replicatorConfig.load(config);
            mongoReplicator = new MongoReplicator(replicatorConfig);
            mongoSource = replicatorConfig.getDataSouce();
            ByteBuffer position = this.context.positionStorageReader().getPosition(ByteBuffer.wrap(
                    mongoSource.getBytes()));

            if (position != null && position.array().length > 0) {
                String positionJson = new String(position.array(), StandardCharsets.UTF_8);
                JSONObject jsonObject = JSONObject.parseObject(positionJson);
                replicatorConfig.setPositionTimeStamp(jsonObject.getLongValue("timeStamp"));
                replicatorConfig.setPositionInc(jsonObject.getIntValue("inc"));
            } else {
                replicatorConfig.setDataSync(Constants.INITIAL);
            }
            mongoReplicator.start();
        }catch (Throwable throwable) {
            logger.info("task start error", throwable);
            stop();
        }
    }

    @Override
    public void stop() {
        logger.info("shut down.....");
        mongoReplicator.shutdown();
    }

    @Override
    public void pause() {
        mongoReplicator.pause();
    }

    @Override
    public void resume() {
        mongoReplicator.resume();
    }

    private void buildFieleds(Schema schema) {
        Field op = new Field(0, Constants.OPERATIONTYPE, FieldType.STRING);
        schema.getFields().add(op);
        Field time = new Field(1, Constants.TIMESTAMP, FieldType.INT64);
        schema.getFields().add(time);
        Field v = new Field(2, Constants.VERSION, FieldType.INT32);
        schema.getFields().add(v);
        Field namespace = new Field(3, Constants.NAMESPACE, FieldType.STRING);
        schema.getFields().add(namespace);
        Field operation = new Field(4, Constants.CREATED, FieldType.STRING);
        schema.getFields().add(operation);
        Field patch = new Field(5, Constants.PATCH, FieldType.STRING);
        schema.getFields().add(patch);
        Field objectId = new Field(6, Constants.OBJECTID, FieldType.STRING);
        schema.getFields().add(objectId);
    }

    private JSONObject position(ReplicationEvent event) {
        JSONObject jsonObject = new JSONObject();
        switch (event.getOperationType()) {
            case CREATED:
                jsonObject.put(Constants.POSITION_TIMESTAMP, 0);
                jsonObject.put(Constants.POSITION_INC, 0);
                jsonObject.put(Constants.INITSYNC, true);
                break;
            default:
                jsonObject.put(Constants.POSITION_TIMESTAMP, event.getTimestamp().getTime());
                jsonObject.put(Constants.POSITION_INC, event.getTimestamp().getInc());
                jsonObject.put(Constants.INITSYNC, false);
                break;
        }
        return jsonObject;

    }
}
