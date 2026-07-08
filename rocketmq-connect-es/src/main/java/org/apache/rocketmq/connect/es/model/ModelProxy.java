package org.apache.rocketmq.connect.es.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.connect.es.config.SyncMetadata;

/**
 * 单表，simple
 * 
 * 主数据必须入es，主数据都oneways noeways manyways 一对一 一
 * 
 * 一对多 一 多
 * 
 * 多对一 一 多
 * 
 * @author laohu
 *
 */
public class ModelProxy implements Model {

	private Map<ModelType, Model> modelMap = new HashMap<ModelType, Model>();

	{
		modelMap.put(ModelType.SIMPLE, new SimpleModel());
		modelMap.put(ModelType.ONEWAYS, new OneWaysModel());
		modelMap.put(ModelType.MANYWAYS, new ManyWaysModel());
		modelMap.put(ModelType.MAIN, new MainModel());

	}

	@Override
	public void create(SyncMetadata syncMetadata) {
		modelMap.get(ModelType.SIMPLE).create(syncMetadata);
		modelMap.get(ModelType.MAIN).create(syncMetadata);
	}

	@Override
	public void update(SyncMetadata syncMetadata) {

		modelMap.get(ModelType.SIMPLE).update(syncMetadata);
		modelMap.get(ModelType.MAIN).update(syncMetadata);
		modelMap.get(ModelType.ONEWAYS).update(syncMetadata);
		modelMap.get(ModelType.MANYWAYS).update(syncMetadata);
	}

	@Override
	public void delete(SyncMetadata syncMetadata) {
		modelMap.get(ModelType.SIMPLE).delete(syncMetadata);
		modelMap.get(ModelType.MAIN).delete(syncMetadata);
	}

}
