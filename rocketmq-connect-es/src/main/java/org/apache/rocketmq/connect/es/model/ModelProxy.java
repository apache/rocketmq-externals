package org.apache.rocketmq.connect.es.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.rocketmq.connect.es.SyncMetadata;
import org.apache.rocketmq.connect.es.config.MapperConfig;

/**
 * 单表，simple
 * 
 * 主数据必须入es，主数据都oneways
 *           noeways        manyways  
 * 一对一       一                   
 * 
 * 一对多       一               多 
 * 
 * 多对一       一               多 
 * @author laohu
 *
 */
public class ModelProxy implements Model {

	private Map<ModelType, Model> modelMap = new HashMap<ModelType, Model>();

	{
		modelMap.put(ModelType.SIMPLE, new SimpleModel());
		modelMap.put(ModelType.ONEWAYS, new OneWaysModel());
		modelMap.put(ModelType.MANYWAYS, new ManyWaysModel());

	}

	@Override
	public void create(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();

		if (Objects.equals(mapperConfig.getMapperType(), ModelType.SIMPLE)) {
			modelMap.get(ModelType.SIMPLE).create(syncMetadata);
		}
		for (MapperConfig newMapperConfig : mapperConfig.getOneWaysapperConfig()) {
			modelMap.get(ModelType.SIMPLE).create(syncMetadata);
		}

		for (MapperConfig newMapperConfig : mapperConfig.getManyWaysMapperConfig()) {
			modelMap.get(ModelType.SIMPLE).create(syncMetadata);
		}
	}

	@Override
	public void update(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();

		if (Objects.equals(mapperConfig.getMapperType(), ModelType.SIMPLE)) {
			modelMap.get(ModelType.SIMPLE).update(syncMetadata);
		}
		for (MapperConfig newMapperConfig : mapperConfig.getOneWaysapperConfig()) {
			modelMap.get(ModelType.SIMPLE).update(syncMetadata);
		}

		for (MapperConfig newMapperConfig : mapperConfig.getManyWaysMapperConfig()) {
			modelMap.get(ModelType.SIMPLE).update(syncMetadata);
		}
	}

	@Override
	public void delete(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();

		if (Objects.equals(mapperConfig.getMapperType(), ModelType.SIMPLE)) {
			modelMap.get(ModelType.SIMPLE).delete(syncMetadata);
		}
		for (MapperConfig newMapperConfig : mapperConfig.getOneWaysapperConfig()) {
			modelMap.get(ModelType.SIMPLE).delete(syncMetadata);
		}

		for (MapperConfig newMapperConfig : mapperConfig.getManyWaysMapperConfig()) {
			modelMap.get(ModelType.SIMPLE).delete(syncMetadata);
		}

	}

}
