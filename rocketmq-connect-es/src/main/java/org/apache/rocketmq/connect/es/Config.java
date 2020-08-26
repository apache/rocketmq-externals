/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.connect.es;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.connect.es.config.ConfigManage;
import org.apache.rocketmq.connect.es.config.ElasticSearchConfig;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.RelationConfig;
import org.apache.rocketmq.connect.es.model.ModelType;
import org.elasticsearch.client.RestHighLevelClient;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.openmessaging.KeyValue;

/**
 * 
 * @author laohu
 *
 */
public class Config {

	ConfigManage configManage = new ConfigManage();

	public void load(KeyValue props) {
		
		BaseConfig baseConfig = getBaseConfig(props);
		if (Objects.isNull(baseConfig)) {

		}

		if (Objects.isNull(baseConfig.getDefaultClient())) {

		}
		configManage.setDefaultElasticSearchConfig(baseConfig.getDefaultClient());
		configManage.setElasticSearchConfig(baseConfig.getSinkclient());
		
		List<MapperConfig> mapperList = baseConfig.getMapper();
		if (Objects.isNull(mapperList) || mapperList.isEmpty()) {
			
		}
		for (MapperConfig mapperConfig : mapperList) {
			RestHighLevelClient restHighLevelClient =  configManage.getRestHighLevelClient(mapperConfig.getClientName());
			if(Objects.isNull(restHighLevelClient)) {
				restHighLevelClient = configManage.getDefaultElasticSearchConfig();
			}
			mapperConfig.setRestHighLevelClient(restHighLevelClient);
			configManage.setMapperConfig(mapperConfig);
		}

		List<RelationConfig> relationList = baseConfig.getRelation();
		for (RelationConfig relationConfig : relationList) {

			MapperConfig mainMapper = relationConfig.getMainMapperConfig();
			String mapperName = mainMapper.getMapperName();
			if (Objects.isNull(mapperName)) {
				// 异常
			}
			MapperConfig currentMainMapper = configManage.getMapperConfigByMapperName(mapperName);

			currentMainMapper.addRelationConfig(relationConfig);

			for (MapperConfig formMapper : relationConfig.getFromMapperConfig()) {
				// 从一对一，需要主的配置
				if (Objects.isNull(formMapper.getMapperType())) {
					//异常
				}
				MapperConfig currentFormMapper = configManage.getMapperConfigByMapperName(formMapper.getMapperName());
				// 这里有一个问题，配置合并，适配等,从数据应该添加
				// 应该是早主数据，而不从配置
				MapperConfig updateMainMapper = new MapperConfig();
				updateMainMapper.setClientName(currentMainMapper.getClientName());
				updateMainMapper.setRestHighLevelClient(currentMainMapper.getRestHighLevelClient());
				updateMainMapper.setMapperName(currentMainMapper.getMapperName());
				updateMainMapper.setIndex(currentMainMapper.getIndex());
				updateMainMapper.setType(currentMainMapper.getType());
				updateMainMapper.setMapperType(currentMainMapper.getMapperType());
				updateMainMapper.setIdPrefix(currentMainMapper.getIdPrefix());
				
				updateMainMapper.setNamingMethod(formMapper.getNamingMethod());
				updateMainMapper.setFieldAndKeyMapper(formMapper.getFieldAndKeyMapper());
				if (updateMainMapper.getMapperType() == ModelType.MANYWAYS && isRelation(updateMainMapper) ) {
					// 把关联字段，换成id
					updateMainMapper.setIdPrefix(formMapper.getMainIdPrefix());
					updateMainMapper.setUniqueName(formMapper.getMainRelationField());
					currentFormMapper.getOneWaysMapperConfig().add(updateMainMapper);
				} else if (updateMainMapper.getMapperType() == ModelType.ONEWAYS) {
					currentFormMapper.getManyWaysMapperConfig().add(updateMainMapper);
				}
				
			}

		}

	}
	
	private boolean isRelation(MapperConfig mapperConfig) {
		return Objects.nonNull(mapperConfig.getMainRelationField());
	}
	

	
	private BaseConfig getBaseConfig(KeyValue props) {
		JSONObject config = new JSONObject();
		Set<String> propsKey = props.keySet();
		for (String key : propsKey) {
			String[] fields = StringUtils.split(key, '.');
			int fieldLength = fields.length;
			JSONObject data = config;
			for (int i = 0; i < fieldLength; i++) {
				String keyName = fields[i];
				if (i < fieldLength - 1) {
					if (keyName.charAt(keyName.length() - 1) == ']') {
						int indexStart = keyName.indexOf('[');
						String dataArrayName = keyName.substring(0, indexStart);
						JSONArray dataArray = data.getJSONArray(dataArrayName);
						if (Objects.isNull(dataArray)) {
							dataArray = new JSONArray();
							data.put(dataArrayName, dataArray);
						}
						JSONObject tmpObject = data.getJSONObject(keyName);
						if (Objects.isNull(tmpObject)) {
							tmpObject = new JSONObject();
							dataArray.add(tmpObject);
							data.put(keyName, tmpObject);
						}
						data = tmpObject;
					} else if (keyName.charAt(keyName.length() - 1) == '}') {
						int indexStart = keyName.indexOf('{');
						String dataObjectName = keyName.substring(0, indexStart);
						JSONObject dataObject = data.getJSONObject(dataObjectName);
						if (Objects.isNull(dataObject)) {
							dataObject = new JSONObject();
							data.put(dataObjectName, dataObject);
						}
						data = dataObject;

					} else {
						JSONObject newData = data.getJSONObject(keyName);
						if (Objects.isNull(newData)) {
							newData = new JSONObject();
							data.put(keyName, newData);
						}
						data = newData;
					}
				} else {
					data.put(keyName, props.getString(key));
				}
			}

		}
		return config.toJavaObject(BaseConfig.class);
	}

	static class BaseConfig {

		private ElasticSearchConfig defaultClient;

		private List<ElasticSearchConfig> sinkclient;

		private List<RelationConfig> relation;

		private List<MapperConfig> mapper;

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
}