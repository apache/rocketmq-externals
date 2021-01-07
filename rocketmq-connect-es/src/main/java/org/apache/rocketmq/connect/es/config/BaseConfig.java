package org.apache.rocketmq.connect.es.config;

import java.util.List;

public  class BaseConfig {

	private String topicNames;
	
	private ElasticSearchConfig defaultClient;

	private List<ElasticSearchConfig> sinkclient;

	private List<RelationConfig> relation;

	private List<MapperConfig> mapper;

	
	
	public String getTopicNames() {
		return topicNames;
	}

	public void setTopicNames(String topicNames) {
		this.topicNames = topicNames;
	}

	public List<ElasticSearchConfig> getSinkclient() {
		return sinkclient;
	}

	public void setSinkclient(List<ElasticSearchConfig> sinkclient) {
		this.sinkclient = sinkclient;
	}

	public List<RelationConfig> getRelation() {
		return relation;
	}

	public void setRelation(List<RelationConfig> relation) {
		this.relation = relation;
	}

	public List<MapperConfig> getMapper() {
		return mapper;
	}

	public void setMapper(List<MapperConfig> mapper) {
		this.mapper = mapper;
	}

	public ElasticSearchConfig getDefaultClient() {
		return defaultClient;
	}

	public void setDefaultClient(ElasticSearchConfig defaultClient) {
		this.defaultClient = defaultClient;
	}

}
