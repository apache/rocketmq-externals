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

package org.apache.rocketmq.connect.kafka.connector;

import com.alibaba.fastjson.JSON;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.*;
import io.openmessaging.connector.api.source.SourceTask;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.rocketmq.connect.kafka.config.ConfigDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class KafkaSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(KafkaSourceTask.class);
    private KafkaConsumer<ByteBuffer, ByteBuffer> consumer;
    private KeyValue config;
    private List<String> topicList;
    private List<TopicPartition> currentTPList;

    @Override
    public Collection<SourceDataEntry> poll() {

        try {
            ArrayList<SourceDataEntry> entries = new ArrayList<>();
            ConsumerRecords<ByteBuffer, ByteBuffer> records = consumer.poll(1000);
            if (records.count() > 0) {
                log.info("consumer.poll, records.count {}", records.count());
            }
            for (ConsumerRecord<ByteBuffer, ByteBuffer> record : records) {
                String topic_partition = record.topic() + "-" + record.partition();
                log.info("Received {} record: {} ", topic_partition, record);

                Schema schema = new Schema();
                List<Field> fields = new ArrayList<>();
                fields.add(new Field(0, "key", FieldType.BYTES));
                fields.add(new Field(1, "value", FieldType.BYTES));
                schema.setName(record.topic());
                schema.setFields(fields);
                schema.setDataSource(record.topic());

                ByteBuffer sourcePartition = ByteBuffer.wrap(topic_partition.getBytes());
                ByteBuffer sourcePosition = ByteBuffer.allocate(8);
                sourcePosition.asLongBuffer().put(record.offset());

                DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                dataEntryBuilder.entryType(EntryType.CREATE);
                dataEntryBuilder.queue(record.topic()); //queueName will be set to RocketMQ topic by runtime
                dataEntryBuilder.timestamp(System.currentTimeMillis());
                if (record.key() != null) {
                    dataEntryBuilder.putFiled("key", JSON.toJSONString(record.key().array()));
                } else {
                    dataEntryBuilder.putFiled("key", null);
                }
                dataEntryBuilder.putFiled("value", JSON.toJSONString(record.value().array()));
                SourceDataEntry entry = dataEntryBuilder.buildSourceDataEntry(sourcePartition, sourcePosition);
                entries.add(entry);
            }

            log.info("poll return entries size {} ", entries.size());
            return entries;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("poll exception {}", e);
        }
        return null;
    }

    @Override
    public void start(KeyValue taskConfig) {
        log.info("source task start enter");
        this.topicList = new ArrayList<>();
        this.currentTPList = new ArrayList<>();
        this.config = taskConfig;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.config.getString(ConfigDefine.BOOTSTRAP_SERVER));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.config.getString(ConfigDefine.GROUP_ID));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteBufferDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteBufferDeserializer");

        this.consumer = new KafkaConsumer<>(props);

        String topics = this.config.getString(ConfigDefine.TOPICS);
        for (String topic : topics.split(",")) {
            if (!topic.isEmpty()) {
                topicList.add(topic);
            }
        }

        consumer.subscribe(topicList, new MyRebalanceListener());
        log.info("source task subscribe topicList {}", topicList);
    }

    @Override
    public void stop() {
        log.info("source task stop enter");
        try {
            commitOffset(currentTPList, true);
            consumer.wakeup(); // wakeup poll in other thread
            consumer.close();
        } catch (Exception e) {
            log.warn("{} consumer {} close exception {}", this, consumer, e);
        }
    }

    @Override
    public void pause() {
        log.info("source task pause ...");
    }

    @Override
    public void resume() {
        log.info("source task resume ...");
    }

    public String toString() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        return "KafkaSourceTask-PID[" + pid + "]-" + Thread.currentThread().toString();
    }

    public static TopicPartition getTopicPartition(ByteBuffer buffer)
    {
        Charset charset = null;
        CharsetDecoder decoder = null;
        CharBuffer charBuffer = null;
        try
        {
            charset = Charset.forName("UTF-8");
            decoder = charset.newDecoder();
            charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
            String topic_partition = charBuffer.toString();
            int index = topic_partition.lastIndexOf('-');
            if (index != -1 && index > 1) {
                String topic = topic_partition.substring(0, index - 1);
                int partition = Integer.parseInt(topic_partition.substring(index + 1));
                return new TopicPartition(topic, partition);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            log.warn("getString Exception {}", ex);
        }
        return null;
    }

    private void commitOffset(Collection<TopicPartition> tpList, boolean isClose) {

        if(tpList == null || tpList.isEmpty())
            return;

        log.info("commitOffset {} topic partition {}", KafkaSourceTask.this, tpList);
        List<ByteBuffer> topic_partition_list = new ArrayList<>();
        for (TopicPartition tp : tpList) {
            topic_partition_list.add(ByteBuffer.wrap((tp.topic()+"-"+tp.partition()).getBytes()));
        }

        Map<TopicPartition, OffsetAndMetadata> commitOffsets = new HashMap<>();
        Map<ByteBuffer, ByteBuffer> topic_position_map = context.positionStorageReader().getPositions(topic_partition_list);
        for (Map.Entry<ByteBuffer, ByteBuffer> entry : topic_position_map.entrySet()) {
            TopicPartition tp = getTopicPartition(entry.getKey());
            if (tp != null && tpList.contains(tp)) {
                //positionStorage store more than this task's topic and partition
                try {
                    long local_offset = entry.getValue().asLongBuffer().get();
                    commitOffsets.put(tp, new OffsetAndMetadata(local_offset));
                } catch (Exception e) {
                    log.warn("commitOffset get local offset exception {}", e);
                }
            }
        }

        commitOffsets.entrySet().stream().forEach((Map.Entry<TopicPartition, OffsetAndMetadata> entry) ->
                log.info("commitOffset {}, TopicPartition:{} offset:{}", KafkaSourceTask.this, entry.getKey(), entry.getValue()));
        if (!commitOffsets.isEmpty()) {
            if (isClose) {
                consumer.commitSync(commitOffsets);
            } else {
                consumer.commitAsync(commitOffsets, new MyOffsetCommitCallback());
            }
        }
    }

    private class MyOffsetCommitCallback implements OffsetCommitCallback {

        @Override
        public void onComplete(Map<TopicPartition, OffsetAndMetadata> map, Exception e) {
            if (e != null) {
                log.warn("commit async excepiton {}", e);
                map.entrySet().stream().forEach((Map.Entry<TopicPartition, OffsetAndMetadata> entry) -> {
                    log.warn("commit exception, TopicPartition:{} offset: {}", entry.getKey().toString(), entry.getValue().offset());
                });
                return;
            }
        }
    }

    private class MyRebalanceListener implements ConsumerRebalanceListener {

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

            currentTPList.clear();
            for (TopicPartition tp : partitions) {
                currentTPList.add(tp);
            }
            currentTPList.stream().forEach((TopicPartition tp)-> log.info("onPartitionsAssigned  TopicPartition {}", tp));
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

            log.info("onPartitionsRevoked {} Partitions revoked", KafkaSourceTask.this);
            try {
                commitOffset(partitions, false);
            } catch (Exception e) {
                log.warn("onPartitionsRevoked exception", e);
            }
        }
    }
}
