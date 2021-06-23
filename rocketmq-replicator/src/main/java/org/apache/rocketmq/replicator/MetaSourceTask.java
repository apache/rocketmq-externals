/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.replicator;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.replicator.common.ConstDefine;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigUtil;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.replicator.offset.OffsetSyncStore;
import org.apache.rocketmq.replicator.schema.FieldName;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceTask.class);

    private final String taskId;
    private final TaskConfig config;
    private DefaultMQAdminExt srcMQAdminExt;
    private volatile boolean started = false;

    private OffsetSyncStore store;

    public MetaSourceTask() {
        this.config = new TaskConfig();
        this.taskId = Utils.createTaskId(Thread.currentThread().getName());
    }

    @Override public void start(KeyValue config) {
        ConfigUtil.load(config, this.config);

        startAdmin();

        this.store = new OffsetSyncStore(this.srcMQAdminExt, this.config);
        this.started = true;
    }

    @Override public void stop() {
        if (started) {
            started = false;
        }
        srcMQAdminExt.shutdown();
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    @Override public Collection<SourceDataEntry> poll() {
        log.debug("polling...");
        List<String> groups = JSONObject.parseArray(this.config.getTaskGroupList(), String.class);

        if (groups == null) {
            log.info("no group in task.");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return Collections.emptyList();
        }
        List<SourceDataEntry> res = new ArrayList<>();
        for (String group : groups) {
            ConsumeStats stats;
            try {
                stats = this.srcMQAdminExt.examineConsumeStats(group);
            } catch (Exception e) {
                log.error("admin get consumer info failed for consumer groups: " + group, e);
                continue;
            }

            for (Map.Entry<MessageQueue, OffsetWrapper> offsetTable : stats.getOffsetTable().entrySet()) {

                MessageQueue mq = offsetTable.getKey();
                long srcOffset = offsetTable.getValue().getConsumerOffset();
                long targetOffset = this.store.convertTargetOffset(mq, srcOffset);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(RmqConstants.NEXT_POSITION, srcOffset);

                Schema schema = new Schema();
                schema.setDataSource(this.config.getSourceRocketmq());
                schema.setName(mq.getTopic());
                schema.setFields(new ArrayList<>());
                schema.getFields().add(new Field(0,
                    FieldName.OFFSET.getKey(), FieldType.INT64));

                DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                dataEntryBuilder.timestamp(System.currentTimeMillis())
                    .queue(this.config.getStoreTopic())
                    .entryType(EntryType.UPDATE);
                dataEntryBuilder.putFiled(FieldName.OFFSET.getKey(), targetOffset);

                SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                    ByteBuffer.wrap(RmqConstants.getPartition(
                        mq.getTopic(),
                        mq.getBrokerName(),
                        String.valueOf(mq.getQueueId())).getBytes(StandardCharsets.UTF_8)),
                    ByteBuffer.wrap(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8))
                );
                String targetTopic = new StringBuilder().append(group).append("-").append(mq.getTopic())
                    .append("-").append(mq.getQueueId()).toString();
                sourceDataEntry.setQueueName(targetTopic);
                res.add(sourceDataEntry);
            }
        }
        return res;
    }

    private void startAdmin() {
        this.srcMQAdminExt = new DefaultMQAdminExt();
        this.srcMQAdminExt.setNamesrvAddr(this.config.getSourceRocketmq());
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(this.config.getSourceRocketmq()));
        try {
            this.srcMQAdminExt.start();
        } catch (MQClientException e) {
            log.error("start src mq admin failed.", e);
            throw new IllegalStateException("start src mq admin failed");
        }
    }
}
