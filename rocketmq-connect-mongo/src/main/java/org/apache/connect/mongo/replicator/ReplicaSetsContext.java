package org.apache.connect.mongo.replicator;

import com.mongodb.client.MongoClient;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private Filter filter;

    private MongoClientFactory mongoClientFactory;

    public ReplicaSetsContext(SourceTaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        this.replicaSets = new CopyOnWriteArrayList<>();
        this.dataEntryQueue = new LinkedBlockingDeque<>();
        this.filter = new Filter(taskConfig);
        this.mongoClientFactory = new MongoClientFactory(taskConfig);
    }

    public MongoClient createMongoClient(ReplicaSetConfig replicaSetConfig) {
        return mongoClientFactory.createMongoClient(replicaSetConfig);
    }

    public boolean filterEvent(ReplicationEvent event) {
        return filter.filterEvent(event);
    }

    public boolean filterMeta(CollectionMeta collectionMeta) {
        return filter.filterMeta(collectionMeta);
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

    public boolean initSyncAbort() {
        return initSyncAbort.get();
    }

    public void initSyncError() {
        initSyncAbort.set(true);
    }

}
