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


package org.apache.rocketmq.connect.hudi.sink;


import io.openmessaging.connector.api.data.SinkDataEntry;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hudi.client.HoodieJavaWriteClient;
import org.apache.hudi.client.WriteStatus;
import org.apache.hudi.client.common.HoodieJavaEngineContext;
import org.apache.hudi.common.engine.EngineType;
import org.apache.hudi.common.fs.FSUtils;
import org.apache.hudi.common.model.HoodieAvroPayload;
import org.apache.hudi.common.model.HoodieKey;
import org.apache.hudi.common.model.HoodieRecord;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.config.HoodieCompactionConfig;
import org.apache.hudi.config.HoodieIndexConfig;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.hudi.index.HoodieIndex;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.avro.GenericDataSupplier;
import org.apache.rocketmq.connect.hudi.config.HudiConnectConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Updater {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private HudiConnectConfig hudiConnectConfig;
    private HoodieJavaWriteClient hudiWriteClient;
    private HoodieWriteConfig cfg;
    private transient ScheduledExecutorService scheduledExecutor;
    private int flushIntervalMs = 3000;
    private int batchSize = 100;
    private List<SinkDataEntry> inflightList;
    private Object batchLocker = new Object();


    public Updater(HudiConnectConfig hudiConnectConfig) throws Exception {
        this.hudiConnectConfig = hudiConnectConfig;

        try {
            File schemaFile = new File(hudiConnectConfig.getSchemaPath());
            this.hudiConnectConfig.schema = new Schema.Parser().parse(schemaFile);
            log.info("Hudi schema : " + this.hudiConnectConfig.schema.toString());
        } catch (IOException e) {
            throw new Exception(String.format("Failed to find schema file %s", hudiConnectConfig.getSchemaPath()), e);
        }
        Configuration hadoopConf = new Configuration();
        hadoopConf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, false);
        hadoopConf.set(AvroReadSupport.AVRO_DATA_SUPPLIER, GenericDataSupplier.class.getName());
        hadoopConf.setClassLoader(this.getClass().getClassLoader());
        hadoopConf.set("fs.hdfs.impl",
                DistributedFileSystem.class.getName()
        );
        hadoopConf.set("fs.file.impl",
                LocalFileSystem.class.getName()
        );

        // fs.%s.impl.disable.cache
        hadoopConf.set("fs.file.impl.disable.cache", String.valueOf(true));
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        Path path = new Path(hudiConnectConfig.getTablePath());
        FileSystem fs = FSUtils.getFs(hudiConnectConfig.getTablePath(), hadoopConf);
        if (!fs.exists(path)) {
            HoodieTableMetaClient.withPropertyBuilder()
                    .setTableType(hudiConnectConfig.getTableType())
                    .setTableName(hudiConnectConfig.getTableName())
                    .setPayloadClassName(HoodieAvroPayload.class.getName())
                    .initTable(hadoopConf, hudiConnectConfig.getTablePath());
        }
        log.info("Hudi inited table");

        this.cfg = HoodieWriteConfig.newBuilder().withPath(hudiConnectConfig.getTablePath())
                .withSchema(this.hudiConnectConfig.schema.toString())
                .withEngineType(EngineType.JAVA)
                .withParallelism(hudiConnectConfig.getInsertShuffleParallelism(), hudiConnectConfig.getUpsertShuffleParallelism())
                .withDeleteParallelism(hudiConnectConfig.getDeleteParallelism()).forTable(hudiConnectConfig.getTableName())
                .withIndexConfig(HoodieIndexConfig.newBuilder().withIndexType(HoodieIndex.IndexType.INMEMORY).build())
                .withCompactionConfig(HoodieCompactionConfig.newBuilder().archiveCommitsWith(20, 30).build()).build();
        cfg.getAvroSchemaValidate();
        this.hudiWriteClient =
                new HoodieJavaWriteClient<HoodieAvroPayload>(new HoodieJavaEngineContext(hadoopConf), cfg);
        log.info("Open HoodieJavaWriteClient successfully");

        inflightList = new ArrayList<>();
        if (batchSize > 0) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        commit();
                    } catch (Exception e) {
                        log.error("Flush error when executed at fixed rate", e);
                    }
                }, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    private GenericRecord sinkDataEntry2GenericRecord(SinkDataEntry record) {
        byte[] recordBytes = (byte[]) record.getPayload()[0];
        GenericRecord genericRecord = new GenericData.Record(this.hudiConnectConfig.schema);
        DatumReader<GenericRecord> userDatumReader = new SpecificDatumReader<GenericRecord>(this.hudiConnectConfig.schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(recordBytes, null);
        try {
            if (!decoder.isEnd()) {
                genericRecord = userDatumReader.read(genericRecord, decoder);
            }
        } catch (IOException e) {
            log.error("SinkDataEntry convert to GenericRecord occur error,", e);
        }
        return genericRecord;
    }

    public boolean push(SinkDataEntry record) {
        log.info("Updater Trying to push data");
        Boolean isSuccess = true;
        if (record == null) {
            log.warn("Updater push sinkDataRecord null.");
            return true;
        }
        synchronized (batchLocker) {
            inflightList.add(record);
        }
        if (inflightList.size() >= batchSize) {
            try {
                scheduledExecutor.submit(this::commit);
            } catch (Exception e) {
                log.error("Updater commmit occur error", e);
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private void schemaEvolution(Schema newSchema, Schema oldSchema) {
        if (null != oldSchema && oldSchema.toString().equals(newSchema.toString())) {
            return;
        }
        log.info("Schema changed. New schema is " + newSchema.toString());
        this.cfg = HoodieWriteConfig.newBuilder().withPath(hudiConnectConfig.getTablePath())
                .withSchema(this.hudiConnectConfig.schema.toString())
                .withEngineType(EngineType.JAVA)
                .withParallelism(hudiConnectConfig.getInsertShuffleParallelism(), hudiConnectConfig.getUpsertShuffleParallelism())
                .withDeleteParallelism(hudiConnectConfig.getDeleteParallelism()).forTable(hudiConnectConfig.getTableName())
                .withIndexConfig(HoodieIndexConfig.newBuilder().withIndexType(HoodieIndex.IndexType.INMEMORY).build())
                .withCompactionConfig(HoodieCompactionConfig.newBuilder().archiveCommitsWith(20, 30).build()).build();
        this.hudiWriteClient.close();
        Configuration hadoopConf = new Configuration();
        hadoopConf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, false);
        hadoopConf.set(AvroReadSupport.AVRO_DATA_SUPPLIER, GenericDataSupplier.class.getName());
        this.hudiWriteClient =
                new HoodieJavaWriteClient<HoodieAvroPayload>(new HoodieJavaEngineContext(hadoopConf), cfg);
    }

    public void commit() {
        List<SinkDataEntry> commitList;
        if (inflightList.isEmpty()) {
            return;
        }
        synchronized (this.inflightList) {
            commitList = inflightList;
            inflightList = new ArrayList<>();
        }
        List<HoodieRecord> hoodieRecordsList = new ArrayList<>();
        for (SinkDataEntry record : commitList) {
            GenericRecord genericRecord = sinkDataEntry2GenericRecord(record);
            HoodieRecord<HoodieAvroPayload> hoodieRecord = new HoodieRecord(new HoodieKey(UUID.randomUUID().toString(), "shardingKey-" + record.getQueueName()), new HoodieAvroPayload(Option.of(genericRecord)));
            hoodieRecordsList.add(hoodieRecord);
        }
        try {
            List<WriteStatus> statuses = hudiWriteClient.upsert(hoodieRecordsList, hudiWriteClient.startCommit());
            log.info("Upserted data to hudi");
            long upserted = statuses.get(0).getStat().getNumInserts();
            if (upserted != commitList.size()) {
                log.warn("Upserted num not equals input");
            }
        } catch (Exception e) {
            log.error("Exception when upserting to Hudi", e);
        }
    }

    public void start() throws Exception {
        log.info("schema load success");
    }

    public void stop() {
        this.hudiWriteClient.close();
        log.info("Hudi sink updater stopped.");
    }

    public HudiConnectConfig getHudiConnectConfig() {
        return hudiConnectConfig;
    }

    public void setHudiConnectConfig(HudiConnectConfig hudiConnectConfig) {
        this.hudiConnectConfig = hudiConnectConfig;
    }

}
