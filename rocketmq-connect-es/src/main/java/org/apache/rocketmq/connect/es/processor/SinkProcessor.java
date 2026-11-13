package org.apache.rocketmq.connect.es.processor;

import org.apache.rocketmq.connect.es.config.SyncMetadata;

public interface SinkProcessor<Response> {

	public void onResponse(Response response , SyncMetadata syncMetadata);

	public void onFailure(Exception e , SyncMetadata syncMetadata);
}
