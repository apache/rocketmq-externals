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

package org.apache.connect.mongo.replicator;

import com.mongodb.client.MongoClient;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.connect.mongo.SourceTaskConfig;
import org.apache.connect.mongo.connector.builder.MongoDataEntry;
import org.apache.connect.mongo.initsync.CollectionMeta;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;

public class ReplicaSetsContext {

    private BlockingQueue<SourceDataEntry> dataEntryQueue;

    private SourceTaskConfig taskConfig;

    private List<ReplicaSet> replicaSets;

    private AtomicBoolean initSyncAbort = new AtomicBoolean();

    private OperationFilter operationFilter;

    private MongoClientFactory mongoClientFactory;


    public ReplicaSetsContext(SourceTaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        this.replicaSets = new ArrayList<>();
        this.dataEntryQueue = new LinkedBlockingDeque<>();
        this.operationFilter = new OperationFilter(taskConfig);
        this.mongoClientFactory = new MongoClientFactory(taskConfig);
    }

    public MongoClient createMongoClient(ReplicaSetConfig replicaSetConfig) {
        return mongoClientFactory.createMongoClient(replicaSetConfig);
    }

    public boolean filterEvent(ReplicationEvent event) {
        return operationFilter.filterEvent(event);
    }

    public boolean filterMeta(CollectionMeta collectionMeta) {
        return operationFilter.filterMeta(collectionMeta);
    }

    public int getCopyThread() {
        return taskConfig.getCopyThread() > 0 ? taskConfig.getCopyThread() : Runtime.getRuntime().availableProcessors();
    }

    public void addReplicaSet(ReplicaSet replicaSet) {
        this.replicaSets.add(replicaSet);
    }

    public void shutdown() {
        replicaSets.forEach(ReplicaSet::shutdown);
    }

    public void pause() {
        replicaSets.forEach(ReplicaSet::pause);
    }

    public void resume() {
        replicaSets.forEach(ReplicaSet::resume);
    }

    public void publishEvent(ReplicationEvent event, ReplicaSetConfig replicaSetConfig) {
        SourceDataEntry sourceDataEntry = MongoDataEntry.createSouceDataEntry(event, replicaSetConfig);
        while (true) {
            try {
                dataEntryQueue.put(sourceDataEntry);
                break;
            } catch (InterruptedException e) {
            }
        }

    }

    public Collection<SourceDataEntry> poll() {
        List<SourceDataEntry> res = new ArrayList<>();
        if (dataEntryQueue.drainTo(res, 20) == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        return res;
    }

    public boolean isInitSyncAbort() {
        return initSyncAbort.get();
    }

    public void setInitSyncError() {
        initSyncAbort.set(true);
    }

}
