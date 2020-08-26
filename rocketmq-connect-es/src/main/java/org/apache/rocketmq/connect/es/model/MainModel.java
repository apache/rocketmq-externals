package org.apache.rocketmq.connect.es.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.rocketmq.connect.es.SyncMetadata;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.RelationConfig;
import org.elasticsearch.action.get.GetResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MainModel extends AbstractModel {

	@Override
	public void create(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();
		List<RelationConfig> relationConfigList = mapperConfig.getRelationConfigList();
		
		
		for(RelationConfig relationConfig : relationConfigList) {
			JSONObject mainData = new JSONObject(syncMetadata.getRowData());
			SyncMetadata mainSyncMetadata = new SyncMetadata(syncMetadata , relationConfig.getMainMapperConfig());
			mainSyncMetadata.setRowData(mainData);
			mainSyncMetadata.setRowBeforeUpdateData(mainData);
			
			AtomicInteger fromLength = new AtomicInteger(relationConfig.getFromMapperConfig().size());
			for(MapperConfig mapper : relationConfig.getFromMapperConfig() ) {
				SyncMetadata fromSyncMetadata = new SyncMetadata(syncMetadata , mapper);
				get(fromSyncMetadata, new ModelDefaultActionListener<GetResponse>(fromSyncMetadata) {
					@Override
					public void onResponse(GetResponse response) {
						super.onResponse(response);
						if(Objects.isNull(response.getSourceAsString())) {
							return;
						}
						mainData.putAll(JSON.parseObject(response.getSourceAsString()));
						if( fromLength.getAndDecrement() > 0) {
							return;
						}
						MainModel.this.create(mainSyncMetadata);
					}
				});
			}
		}
	}
	
	@Override
	public void delete(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();
		List<RelationConfig> relationConfigList = mapperConfig.getRelationConfigList();
		
		
		for(RelationConfig relationConfig : relationConfigList) {
			super.delete(new SyncMetadata(syncMetadata, relationConfig.getMainMapperConfig()));
		}
	}
}
