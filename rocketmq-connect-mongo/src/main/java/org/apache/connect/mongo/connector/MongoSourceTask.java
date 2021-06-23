/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.connect.mongo.connector;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.apache.connect.mongo.SourceTaskConfig;
import org.apache.connect.mongo.replicator.Position;
import org.apache.connect.mongo.replicator.ReplicaSet;
import org.apache.connect.mongo.replicator.ReplicaSetManager;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoSourceTask extends SourceTask {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SourceTaskConfig sourceTaskConfig;

    private ReplicaSetManager replicaSetManager;

    private ReplicaSetsContext replicaSetsContext;

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

            replicaSetManager = ReplicaSetManager.create(sourceTaskConfig.getMongoAddr());

            replicaSetManager.getReplicaConfigByName().forEach((replicaSetName, replicaSetConfig) -> {
                ByteBuffer byteBuffer = this.context.positionStorageReader().getPosition(ByteBuffer.wrap(
                    replicaSetName.getBytes()));
                if (byteBuffer != null && byteBuffer.array().length > 0) {
                    String positionJson = new String(byteBuffer.array(), StandardCharsets.UTF_8);
                    Position position = JSONObject.parseObject(positionJson, Position.class);
                    replicaSetConfig.setPosition(position);
                } else {
                    Position position = new Position();
                    position.setTimeStamp(sourceTaskConfig.getPositionTimeStamp());
                    position.setInc(sourceTaskConfig.getPositionInc());
                    position.setInitSync(sourceTaskConfig.isDataSync());
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
