package org.apache.connect.mongo.replicator;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.connect.mongo.replicator.Constants.MONGO_LOCAL_DATABASE;
import static org.apache.connect.mongo.replicator.Constants.MONGO_OPLOG_RS;


public class ReplicaSet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private AtomicBoolean running = new AtomicBoolean();

    private ReplicaSetConfig replicaSetConfig;


    private ReplicaSetsContext replicaSetsContext;

    private MongoClient mongoClient;

    private ExecutorService executorService;

    private volatile boolean pause = false;

    public ReplicaSet(ReplicaSetConfig replicaSetConfig, ReplicaSetsContext replicaSetsContext) {
        this.replicaSetConfig = replicaSetConfig;
        this.replicaSetsContext = replicaSetsContext;
        this.executorService = Executors.newSingleThreadExecutor((r) -> new Thread(r, "real_time_replica_" + replicaSetConfig.getReplicaSetName() + "thread"));

    }

    public void start() {

        try {
            if (!running.compareAndSet(false, true)) {
                logger.info("the java mongo replica already start");
                return;
            }
            this.mongoClient = replicaSetsContext.createMongoClient(replicaSetConfig);
            this.isReplicaMongo();
            executorService.submit(new ReplicatorTask(this, mongoClient, replicaSetConfig, replicaSetsContext));
        } catch (Exception e) {
            logger.error("start replicator:{} error", replicaSetConfig, e);
            shutdown();
        }
    }


    public boolean isReplicaMongo() {
        MongoDatabase local = mongoClient.getDatabase(MONGO_LOCAL_DATABASE);
        MongoIterable<String> collectionNames = local.listCollectionNames();
        MongoCursor<String> iterator = collectionNames.iterator();
        while (iterator.hasNext()) {
            if (StringUtils.equals(MONGO_OPLOG_RS, iterator.next())) {
                return true;
            }
        }
        this.shutdown();
        throw new IllegalStateException(String.format("url:%s,  is not replica", replicaSetConfig.getHost()));
    }

    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            if (!this.executorService.isShutdown()) {
                executorService.shutdown();
            }
            if (this.mongoClient != null) {
                this.mongoClient.close();
            }
        }

    }


    public void pause() {
        pause = true;
    }

    public void resume() {
        pause = false;
    }

    public boolean isPause() {
        return pause;
    }

    public boolean isRuning() {
        return running.get();
    }
}
