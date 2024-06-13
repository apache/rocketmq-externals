package org.apache.rocketmq.connect.es.config;

import java.util.List;

public class RelationConfig {

	private String relationName;
	
	private MapperConfig mainMapperConfig;
	
	private List<MapperConfig> fromMapperConfig;

	public String getRelationName() {
		return relationName;
	}

	public void setRelationName(String relationName) {
		this.relationName = relationName;
	}

	public MapperConfig getMainMapperConfig() {
		return mainMapperConfig;
	}

	public void setMainMapperConfig(MapperConfig mainMapperConfig) {
		this.mainMapperConfig = mainMapperConfig;
	}

	public List<MapperConfig> getFromMapperConfig() {
		return fromMapperConfig;
	}

	public void setFromMapperConfig(List<MapperConfig> fromMapperConfig) {
		this.fromMapperConfig = fromMapperConfig;
	}
	
	
}
