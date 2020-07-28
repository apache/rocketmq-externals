package org.apache.rocketmq.connect.es.model;

import java.util.List;
import java.util.Objects;

import org.apache.rocketmq.connect.es.SyncMetadata;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.elasticsearch.action.get.GetResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * create 需要找到main
 * update  也要操作main ，
 * 删除自己 就行了
 * @author laohu
 *
 */
public class ManyWaysModel extends AbstractModel {

	@Override
	public void create(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();
		List<MapperConfig> mapperConfigMapperConfig= mapperConfig.getOneWaysapperConfig();
		for(MapperConfig oneWayMapperConfig : mapperConfigMapperConfig ) {
			// 然后在添加
			SyncMetadata oneSyncMetadata = new SyncMetadata(syncMetadata , oneWayMapperConfig);
			get(oneSyncMetadata, new ModelDefaultActionListener<GetResponse>(oneSyncMetadata) {
				@Override
				public void onResponse(GetResponse response) {
					super.onResponse(response);
					if(Objects.isNull(response.getSourceAsString())) {
						return;
					}
					SyncMetadata oneSyncMetadata = new SyncMetadata(syncMetadata , mapperConfig);
					JSONObject rowData = oneSyncMetadata.getRowData();
					rowData.putAll(JSON.parseObject(response.getSourceAsString()));
					
					ManyWaysModel.this.create(oneSyncMetadata);
				}
			});
		}

	}
}
