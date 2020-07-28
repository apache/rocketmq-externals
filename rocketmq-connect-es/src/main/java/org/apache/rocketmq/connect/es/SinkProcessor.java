package org.apache.rocketmq.connect.es;

public interface SinkProcessor<Response> {

	public void onResponse(Response response , SyncMetadata syncMetadata);

	public void onFailure(Exception e , SyncMetadata syncMetadata);
}
