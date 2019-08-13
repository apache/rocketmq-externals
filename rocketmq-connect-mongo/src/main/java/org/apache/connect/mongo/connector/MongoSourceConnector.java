package org.apache.connect.mongo.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import org.apache.connect.mongo.SourceTaskConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MongoSourceConnector extends SourceConnector {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private KeyValue keyValueConfig;


    @Override
    public String verifyAndSetConfig(KeyValue config) {
        for (String requestKey : SourceTaskConfig.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        this.keyValueConfig = config;
        return "";
    }

    @Override
    public void start() {
        logger.info("start mongo source connector:{}", keyValueConfig);
    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return MongoSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        List<KeyValue> config = new ArrayList<>();
        config.add(this.keyValueConfig);
        return config;
    }
}
