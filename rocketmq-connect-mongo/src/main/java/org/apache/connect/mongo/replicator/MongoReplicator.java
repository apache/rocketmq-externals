package org.apache.connect.mongo.replicator;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.connect.mongo.MongoReplicatorConfig;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.connect.mongo.replicator.Constants.*;


public class MongoReplicator {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private AtomicBoolean running = new AtomicBoolean();

    private MongoReplicatorConfig mongoReplicatorConfig;

    private MongoClientSettings clientSettings;

    private ConnectionString connectionString;

    private MongoClient mongoClient;

    private BlockingQueue<ReplicationEvent> queue = new LinkedBlockingQueue<>();

    private Filter filter;

    private ExecutorService executorService;

    private volatile boolean pause = false;

    public MongoReplicator(MongoReplicatorConfig mongoReplicatorConfig) {
        this.mongoReplicatorConfig = mongoReplicatorConfig;
        this.filter = new Filter(mongoReplicatorConfig);
        this.executorService = Executors.newSingleThreadExecutor((r) ->new Thread(r, "real_time_replica_thread"));

        buildConnectionString();
    }

    public void start() {

        try {
            if (!running.compareAndSet(false, true)) {
                logger.info("the java mongo replica already start");
                return;
            }

            this.clientSettings = MongoClientSettings.builder().applicationName(APPLICATION_NAME)
                    .applyConnectionString(connectionString)
                    .build();
            this.mongoClient = MongoClients.create(clientSettings);
            this.isReplicaMongo();
            executorService.submit(new ReplicatorTask(this, mongoClient, mongoReplicatorConfig, filter));
        }catch (Exception e) {
            logger.info("start replicator error", e);
            shutdown();
        }
    }


    private void buildConnectionString() {
        checkConfig();
        StringBuilder sb = new StringBuilder();
        sb.append("mongodb://");
        if (StringUtils.isNotBlank(mongoReplicatorConfig.getMongoUserName())
                && StringUtils.isNotBlank(mongoReplicatorConfig.getMongoPassWord())) {
            sb.append(mongoReplicatorConfig.getMongoUserName());
            sb.append(":");
            sb.append(mongoReplicatorConfig.getMongoPassWord());
            sb.append("@");

        }
        sb.append(mongoReplicatorConfig.getMongoAddr());
        sb.append(":");
        sb.append(mongoReplicatorConfig.getMongoPort());

        this.connectionString = new ConnectionString(sb.toString());
    }

    private void checkConfig() {
        Validate.notBlank(mongoReplicatorConfig.getMongoAddr(), "mongo url is blank");
        Validate.isTrue(mongoReplicatorConfig.getMongoPort() > 0 && mongoReplicatorConfig.getMongoPort() < 65535, "mongo port should >0 and <65535");

    }

    private boolean isReplicaMongo() {
        MongoDatabase local = mongoClient.getDatabase(MONGO_LOCAL_DATABASE);
        MongoIterable<String> collectionNames = local.listCollectionNames();
        for (String collectionName : collectionNames) {
            if (MONGO_OPLOG_RS.equals(collectionName)) {
                return true;
            }
        }
        this.shutdown();
        throw new IllegalStateException(String.format("url:%s, port:%s is not replica", mongoReplicatorConfig.getMongoAddr(), mongoReplicatorConfig.getMongoPort()));
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

    public void publishEvent(ReplicationEvent replicationEvent) {
        while (true) {
            try {
                queue.put(replicationEvent);
                break;
            } catch (Exception e) {
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

    public BlockingQueue<ReplicationEvent> getQueue() {
        return queue;
    }
}
