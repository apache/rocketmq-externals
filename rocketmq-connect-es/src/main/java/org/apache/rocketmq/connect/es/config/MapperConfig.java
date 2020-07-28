package org.apache.rocketmq.connect.es.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.rocketmq.connect.es.model.ModelType;

public class MapperConfig {

	
	private Map<String ,String> mapper;
	
	private ModelType mapperType;
	
	private boolean isSubTable;
	
	private List<String> idList;
	
	private String idPrefix;
	
	private String tableName;
	
	private String queueName;
	
	private String index;
	
	private List<MapperConfig> oneWaysapperConfig = new ArrayList<MapperConfig>();
	
	private List<MapperConfig> manyWaysMapperConfig = new ArrayList<>();
	
	
	private List<String> subTableName = new CopyOnWriteArrayList<String>();

	public Map<String, String> getMapper() {
		return mapper;
	}

	public void setMapper(Map<String, String> mapper) {
		this.mapper = mapper;
	}

	public List<String> getIdList() {
		return idList;
	}

	public void setIdList(List<String> idList) {
		this.idList = idList;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getIdPrefix() {
		return idPrefix;
	}

	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}
	
	

	public boolean isSubTable() {
		return isSubTable;
	}

	public void setSubTable(boolean isSubTable) {
		this.isSubTable = isSubTable;
	}

	public List<String> getSubTableName() {
		return subTableName;
	}

	public ModelType getMapperType() {
		return mapperType;
	}

	public void setMapperType(ModelType mapperType) {
		this.mapperType = mapperType;
	}

	public void addSubTableName(String subTableName) {
		this.subTableName.add(subTableName);
	}

	public List<MapperConfig> getOneWaysapperConfig() {
		return oneWaysapperConfig;
	}

	public void setOneWaysapperConfig(List<MapperConfig> oneWaysapperConfig) {
		this.oneWaysapperConfig = oneWaysapperConfig;
	}

	public List<MapperConfig> getManyWaysMapperConfig() {
		return manyWaysMapperConfig;
	}

	public void setManyWaysMapperConfig(List<MapperConfig> manyWaysMapperConfig) {
		this.manyWaysMapperConfig = manyWaysMapperConfig;
	}
	
	

}
