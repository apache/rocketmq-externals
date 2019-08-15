package org.apache.connect.mongo.connector;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.SourceTaskConfig;
import org.apache.connect.mongo.replicator.Constants;
import org.apache.connect.mongo.replicator.ReplicaSet;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSets;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoSourceTask extends SourceTask {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SourceTaskConfig sourceTaskConfig;

    private ReplicaSets replicaSets;

    private ReplicaSetsContext replicaSetsContext;

    private Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");

    @Override
    public Collection<SourceDataEntry> poll() {

        return replicaSetsContext.poll();
    }

    @Override
    public void start(KeyValue config) {
        try {
            sourceTaskConfig = new SourceTaskConfig();
            sourceTaskConfig.load(config);
            replicaSetsContext = new ReplicaSetsContext(sourceTaskConfig);
            replicaSets = ReplicaSets.create(sourceTaskConfig.getMongoAddr());
            replicaSets.getReplicaConfigByName().forEach((replicaSetName, replicaSetConfig) -> {
                ByteBuffer byteBuffer = this.context.positionStorageReader().getPosition(ByteBuffer.wrap(
                    replicaSetName.getBytes()));
                if (byteBuffer != null && byteBuffer.array().length > 0) {
                    String positionJson = new String(byteBuffer.array(), StandardCharsets.UTF_8);
                    ReplicaSetConfig.Position position = JSONObject.parseObject(positionJson, ReplicaSetConfig.Position.class);
                    replicaSetConfig.setPosition(position);
                } else {
                    ReplicaSetConfig.Position position = replicaSetConfig.emptyPosition();
                    position.setTimeStamp(sourceTaskConfig.getPositionTimeStamp() != null
                        && pattern.matcher(sourceTaskConfig.getPositionTimeStamp()).matches()
                        ? Integer.valueOf(sourceTaskConfig.getPositionTimeStamp()) : 0);
                    position.setInc(sourceTaskConfig.getPositionInc() != null
                        && pattern.matcher(sourceTaskConfig.getPositionInc()).matches()
                        ? Integer.valueOf(sourceTaskConfig.getPositionInc()) : 0);
                    position.setInitSync(StringUtils.equals(sourceTaskConfig.getDataSync(), Constants.INITSYNC) ? true : false);
                    replicaSetConfig.setPosition(position);
                }

                ReplicaSet replicaSet = new ReplicaSet(replicaSetConfig, replicaSetsContext);
                replicaSetsContext.addReplicaSet(replicaSet);
                replicaSet.start();
            });

        } catch (Throwable throwable) {
            logger.error("task start error", throwable);
            stop();
        }
    }

    @Override
    public void stop() {
        logger.info("shut down.....");
        replicaSetsContext.shutdown();
    }

    @Override
    public void pause() {
        logger.info("pause replica task...");
        replicaSetsContext.pause();
    }

    @Override
    public void resume() {
        logger.info("resume replica task...");
        replicaSetsContext.resume();
    }

}
