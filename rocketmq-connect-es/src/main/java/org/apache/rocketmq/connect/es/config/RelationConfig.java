package org.apache.rocketmq.connect.es.config;

import org.apache.rocketmq.connect.es.model.ModelType;

public class RelationConfig {

	private ModelType modelType;
	
	private MapperConfig mainMapperConfig;
	
	private MapperConfig fromMapperConfig;
}
