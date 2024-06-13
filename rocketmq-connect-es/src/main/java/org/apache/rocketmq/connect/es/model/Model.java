package org.apache.rocketmq.connect.es.model;

import org.apache.rocketmq.connect.es.config.SyncMetadata;

public interface Model {

	public void create(SyncMetadata syncMetadata);

	public void update(SyncMetadata syncMetadata);

	public void delete(SyncMetadata syncMetadata);
}
