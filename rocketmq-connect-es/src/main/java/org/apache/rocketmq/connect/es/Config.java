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
import org.apache.rocketmq.connect.es.config.BaseConfig;
import org.apache.rocketmq.connect.es.config.ConfigManage;
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

	private ConfigManage configManage;

	public Config(ConfigManage configManage) {
		this.configManage = configManage;
	}
	
	public void load(KeyValue props) {
		
		BaseConfig baseConfig = getBaseConfig(props);
		handle(baseConfig);

	}
	
	public void handle(BaseConfig baseConfig ) {
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
		if(Objects.isNull(relationList)) {
			return;
		}
		for (RelationConfig relationConfig : relationList) {

			MapperConfig mainMapper = relationConfig.getMainMapperConfig();
			String mapperName = mainMapper.getMapperName();
			if (Objects.isNull(mapperName)) {
				// 异常
			}
			MapperConfig currentMainMapper = configManage.getMapperConfigByMapperName(mapperName);
			mainMapper.setUniqueName(currentMainMapper.getUniqueName());
			if(Objects.isNull(mainMapper.getIndex())) {
				mainMapper.setIndex(currentMainMapper.getIndex());
			}
			RestHighLevelClient restHighLevelClient =  configManage.getRestHighLevelClient(currentMainMapper.getClientName());
			if(Objects.isNull(restHighLevelClient)) {
				restHighLevelClient = configManage.getDefaultElasticSearchConfig();
			}
			mainMapper.setRestHighLevelClient(restHighLevelClient);
			
			currentMainMapper.addRelationConfig(relationConfig);

			for (MapperConfig formMapper : relationConfig.getFromMapperConfig()) {
				if (Objects.isNull(formMapper.getMapperName())) {
					//异常
				}
				// 从一对一，需要主的配置
				if (Objects.isNull(formMapper.getMapperType())) {
					//异常
				}
				if (Objects.isNull(formMapper.getMainRelationField())) {
					//异常
				}
				
				MapperConfig currentFormMapper = configManage.getMapperConfigByMapperName(formMapper.getMapperName());
				
				// main create的时候需要查询
				formMapper.setUniqueName(formMapper.getMainRelationField());
				formMapper.setRestHighLevelClient(currentFormMapper.getRestHighLevelClient());
				formMapper.setIndex(currentFormMapper.getIndex());
				
				MapperConfig updateMainMapper = new MapperConfig();
				// 这里是在oneModel与manyModel的update的时候可以找到组合数据
				updateMainMapper.setClientName(mainMapper.getClientName());
				updateMainMapper.setRestHighLevelClient(mainMapper.getRestHighLevelClient());
				updateMainMapper.setMapperName(currentMainMapper.getMapperName());
				updateMainMapper.setIndex(mainMapper.getIndex());
				updateMainMapper.setType(mainMapper.getType());
				updateMainMapper.setMapperType(formMapper.getMapperType());
				// 1. 字段名冲突或
				// 2. 字段名有意义
				updateMainMapper.setNamingMethod(formMapper.getNamingMethod());
				updateMainMapper.setFieldAndKeyMapper(formMapper.getFieldAndKeyMapper());
				if (updateMainMapper.getMapperType() == ModelType.ONEWAYS  ) {
					// 通过 from数据的id 查询数据，
					updateMainMapper.setMainRelationField(formMapper.getMainRelationField());
					updateMainMapper.setIdPrefix(currentMainMapper.getIdPrefix());
					updateMainMapper.setUniqueName(currentMainMapper.getUniqueName());
					currentFormMapper.getOneWaysMapperConfig().add(updateMainMapper);
				} else  {
					updateMainMapper.setMapperType(ModelType.MANYWAYS);
					// 通过mainIdPrefix 与 RelationField 生成组合数据id 找到组合数据，并且修改
					updateMainMapper.setUniqueName(currentFormMapper.getUniqueName());
					updateMainMapper.setMainRelationField(formMapper.getMainRelationField());
					currentFormMapper.getManyWaysMapperConfig().add(updateMainMapper);
				}
				
			}

		}
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
}