package org.apache.rocketmq.connect.es.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.RelationConfig;
import org.apache.rocketmq.connect.es.config.SyncMetadata;
import org.elasticsearch.action.get.GetResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MainModel extends AbstractModel {

	@Override
	public void create(SyncMetadata syncMetadata) {

		for (RelationConfig relationConfig : syncMetadata.getMapperConfig().getRelationConfigList()) {
			// 线程安全
			JSONObject mainData = new JSONObject(syncMetadata.getRowData());
			SyncMetadata mainSyncMetadata = new SyncMetadata(syncMetadata, relationConfig.getMainMapperConfig());
			mainSyncMetadata.setRowData(mainData);
			mainSyncMetadata.setRowBeforeUpdateData(mainData);

			AtomicInteger fromLength = new AtomicInteger(relationConfig.getFromMapperConfig().size());
			for (MapperConfig mapper : relationConfig.getFromMapperConfig()) {
				SyncMetadata fromSyncMetadata = new SyncMetadata(syncMetadata, mapper);
				get(fromSyncMetadata, new ModelDefaultActionListener<GetResponse>(fromSyncMetadata) {
					@Override
					public void onResponse(GetResponse response) {
						super.onResponse(response);
						if (Objects.isNull(response.getSourceAsString())) {
							return;
						}
						JSONObject fromData = JSON.parseObject(response.getSourceAsString());
						fromData.remove(mapper.getUniqueName());

						synchronized (mainData) {
							mainData.putAll(fromData);
						}
						if (fromLength.decrementAndGet() > 0) {
							return;
						}
						MainModel.super.create(mainSyncMetadata);
					}
				});
			}
		}
	}

	@Override
	public void update(SyncMetadata syncMetadata) {
		JSONObject rowBeforeUpdateData = syncMetadata.getRowBeforeUpdateData();
		JSONObject rowData = syncMetadata.getRowData();
		for (RelationConfig relationConfig : syncMetadata.getMapperConfig().getRelationConfigList()) {
			// 找到数据主键 与主数据之间的关联字段
			JSONObject mainData = new JSONObject(rowData);
			SyncMetadata mainSyncMetadata = new SyncMetadata(syncMetadata, relationConfig.getMainMapperConfig());
			mainSyncMetadata.setRowData(mainData);
			mainSyncMetadata.setRowBeforeUpdateData(rowBeforeUpdateData);

			AtomicInteger fromLength = new AtomicInteger(relationConfig.getFromMapperConfig().size());

			for (MapperConfig fromMapperConfig : relationConfig.getFromMapperConfig()) {
				String mainRelationField = fromMapperConfig.getMainRelationField();
				String beforeData = rowBeforeUpdateData.getString(mainRelationField);
				String data = rowData.getString(mainRelationField);
				SyncMetadata fromSyncMetadata = new SyncMetadata(syncMetadata, fromMapperConfig);
				if (StringUtils.equals(beforeData, data)) {
					fromLength.decrementAndGet();
					continue;
				}
				get(fromSyncMetadata, new ModelDefaultActionListener<GetResponse>(fromSyncMetadata) {
					@Override
					public void onResponse(GetResponse response) {
						super.onResponse(response);
						if (Objects.isNull(response.getSourceAsString())) {
							return;
						}
						JSONObject fromData = JSON.parseObject(response.getSourceAsString());
						fromData.remove(fromMapperConfig.getUniqueName());
						
						synchronized (mainData) {
							mainData.putAll(fromData);
						}
						if (fromLength.decrementAndGet() > 0) {
							return;
						}
						MainModel.super.update(mainSyncMetadata);
					}
				});
			}
		}
	}

	@Override
	public void delete(SyncMetadata syncMetadata) {
		for (RelationConfig relationConfig : syncMetadata.getMapperConfig().getRelationConfigList()) {
			super.delete(new SyncMetadata(syncMetadata, relationConfig.getMainMapperConfig()));
		}
	}
}
