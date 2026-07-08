package org.apache.rocketmq.connect.es.connector;

import java.util.ArrayList;
import java.util.List;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.sink.SinkConnector;

public class EsSinkConnector extends SinkConnector {

	private KeyValue config;
	
	@Override
	public String verifyAndSetConfig(KeyValue config) {
		this.config = config;
		return null;
	}

	@Override
	public void start() {

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
		return EsSinkTask.class;
	}

	@Override
	public List<KeyValue> taskConfigs() {
        List<KeyValue> config = new ArrayList<>();
        config.add(this.config);
        return config;
	}

}
