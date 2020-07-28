package org.apache.rocketmq.connect.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.elasticsearch.client.RestHighLevelClient;

import com.alibaba.fastjson.JSONObject;

import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.SinkDataEntry;

public class SyncMetadata {
	
	private JSONObject rowData;
	
	private JSONObject rowBeforeUpdateData;
	
	private SinkDataEntry sinkDataEntry;
	
	private RestHighLevelClient client;
	
	private MapperConfig mapperConfig;
	
	private List<SinkProcessor<Object>> resultProcessing = new ArrayList<>();
	
	
	public SyncMetadata() {}
	
	public SyncMetadata(SyncMetadata syncMetadata, MapperConfig mapperConfig) {
		this.rowData = syncMetadata.rowData;
		this.rowBeforeUpdateData = syncMetadata.rowBeforeUpdateData;
		this.sinkDataEntry = syncMetadata.sinkDataEntry;
		this.mapperConfig = mapperConfig;
	}
	
	public JSONObject getRowData() {
		return rowData;
	}

	public void setRowData(JSONObject rowData) {
		this.rowData = rowData;
	}

	public JSONObject getRowBeforeUpdateData() {
		return rowBeforeUpdateData;
	}

	public void setRowBeforeUpdateData(JSONObject rowBeforeUpdateData) {
		this.rowBeforeUpdateData = rowBeforeUpdateData;
	}

	public SinkDataEntry getSinkDataEntry() {
		return sinkDataEntry;
	}

	public void setSinkDataEntry(SinkDataEntry sinkDataEntry) {
		this.sinkDataEntry = sinkDataEntry;
	}

	public RestHighLevelClient getClient() {
		return client;
	}

	public void setClient(RestHighLevelClient client) {
		this.client = client;
	}

	public String getQueueName() {
        return sinkDataEntry.getQueueName();
    }


    public String  getTableName() {
        return sinkDataEntry.getSchema().getName();
    }

	public MapperConfig getMapperConfig() {
		return mapperConfig;
	}

	public void setMapperConfig(MapperConfig mapperConfig) {
		this.mapperConfig = mapperConfig;
	}
    
    public String getIndex() {
    	return mapperConfig.getIndex();
    }
    
	public List<SinkProcessor<Object>> getResultProcessing() {
		return resultProcessing;
	}

	public void setResultProcessing(List<SinkProcessor<Object>> resultProcessing) {
		this.resultProcessing = resultProcessing;
	}

	public String getId() {
    	JSONObject data;
    	if(Objects.equals(EntryType.CREATE, sinkDataEntry.getEntryType())) {
    		data = rowData;
    	}else if(Objects.equals(EntryType.UPDATE, sinkDataEntry.getEntryType())) {
    		data = rowData;
    	}else {
    		data = rowBeforeUpdateData;
    	}
    	StringBuffer sb = new StringBuffer();
    	sb.append(mapperConfig.getIdPrefix());
    	for(String key : mapperConfig.getIdList() ) {
    		sb.append(data.getString(key));
    	}
    	return sb.toString();
    }

}
