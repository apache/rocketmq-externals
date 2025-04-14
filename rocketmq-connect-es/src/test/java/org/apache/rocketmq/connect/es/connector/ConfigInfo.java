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
package org.apache.rocketmq.connect.es.connector;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.connect.es.config.BaseConfig;
import org.apache.rocketmq.connect.es.config.ElasticSearchConfig;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.RelationConfig;
import org.apache.rocketmq.connect.es.model.ModelType;

public class ConfigInfo {
	
	static String[] mapperKeys = new String[] { "student", "class", "teacher", "classroom","relation" };

	static String[] setMapperKeys = new String[] { "student_class", "student_teacher_class", "teacher_class",
			"class_classroom" };

	
	
	/**
	 * 学生，班级，老师   </br>
	 * 学生与班级 多对一  </br>
	 * 老师与班级 多对一  </br>
	 * 学生，班级，老师 是 多，一，多 , 组合表 </br>
	 * 班级与教室 一对一   </br>
	 * 
	 * @return
	 */
	public static BaseConfig simulation() {
		BaseConfig baseConfig = new BaseConfig();
		simulationMapper(baseConfig);
		simulationClient(baseConfig);
		List<RelationConfig> relationConfigList = new ArrayList<>();
		baseConfig.setRelation(relationConfigList);

		simulationStudentClassRelation(baseConfig);
		simulationRelation(baseConfig);
		return baseConfig;
	}

	private static void simulationClient(BaseConfig baseConfig) {
		List<ElasticSearchConfig> elasticSearchConfigList = new ArrayList<>();
		for (String mapperKey : mapperKeys) {
			ElasticSearchConfig elasticSearchConfig = new ElasticSearchConfig();
			elasticSearchConfig.setName(mapperKey + "_client");
			elasticSearchConfig.setServerAddress("http://127.0.0.1:9200");
			elasticSearchConfigList.add(elasticSearchConfig);
		}
		for (String mapperKey : setMapperKeys) {
			ElasticSearchConfig elasticSearchConfig = new ElasticSearchConfig();
			elasticSearchConfig.setName(mapperKey + "_client");
			elasticSearchConfig.setServerAddress("http://127.0.0.1:9200");
			elasticSearchConfigList.add(elasticSearchConfig);
		}
		baseConfig.setSinkclient(elasticSearchConfigList);
		ElasticSearchConfig elasticSearchConfig = new ElasticSearchConfig();
		elasticSearchConfig.setServerAddress("http://127.0.0.1:9200");
		baseConfig.setDefaultClient(elasticSearchConfig);
	}

	private static void simulationMapper(BaseConfig baseConfig) {
		List<MapperConfig> mapperList = new ArrayList<>();
		for (String mapperKey : mapperKeys) {
			MapperConfig mapperConfig = new MapperConfig();
			mapperConfig.setMapperName(mapperKey + "Mapper");
			mapperConfig.setTableName(mapperKey);
			mapperConfig.setClientName(mapperKey + "_client");
			if("relation".equals(mapperKey)) {
				mapperConfig.setUniqueName("relation_id");
			}else {
				mapperConfig.setUniqueName("id");
			}
			mapperConfig.setIndex(mapperKey + "_index");
			mapperConfig.setType(mapperKey + "_type");
			mapperList.add(mapperConfig);
		}
		baseConfig.setMapper(mapperList);
	}

	private  static void simulationStudentClassRelation(BaseConfig baseConfig) {

	/*	RelationConfig relationConfig = new RelationConfig();
		baseConfig.getRelation().add(relationConfig);

		// 学生与班级
		MapperConfig mainMapperConfig = new MapperConfig();
		mainMapperConfig.setMapperName("classMapper");
		mainMapperConfig.setClientName("student_class_client");
		mainMapperConfig.setIndex("student_class_index");
		mainMapperConfig.setType("student_class_type");

		relationConfig.setRelationName("student_class");
		relationConfig.setMainMapperConfig(mainMapperConfig);

		List<MapperConfig> fromMapperConfigList = new ArrayList<>();
		MapperConfig fromMapperConfig = new MapperConfig();
		fromMapperConfig.setMapperName("studentMapper");
		fromMapperConfig.setMainRelationField("class_id");
		fromMapperConfig.setMapperType(ModelType.ONEWAYS);
		fromMapperConfigList.add(fromMapperConfig);
		relationConfig.setFromMapperConfig(fromMapperConfigList);*/
	}

	private static void simulationRelation(BaseConfig baseConfig) {

		RelationConfig relationConfig = new RelationConfig();
		baseConfig.getRelation().add(relationConfig);

		// 关系
		MapperConfig mainMapperConfig = new MapperConfig();
		mainMapperConfig.setMapperName("relationMapper");
		mainMapperConfig.setClientName("relation_client");
		mainMapperConfig.setIndex("relation_structure_index");
		
		relationConfig.setRelationName("relation_structure");
		relationConfig.setMainMapperConfig(mainMapperConfig);

		List<MapperConfig> fromMapperConfigList = new ArrayList<>();
		MapperConfig fromMapperConfig = new MapperConfig();
		fromMapperConfig.setMapperName("studentMapper");
		fromMapperConfig.setMainRelationField("s_id");
		fromMapperConfig.setMapperType(ModelType.ONEWAYS);
		fromMapperConfigList.add(fromMapperConfig);
		relationConfig.setFromMapperConfig(fromMapperConfigList);
		
		fromMapperConfig = new MapperConfig();
		fromMapperConfig.setMapperName("teacherMapper");
		fromMapperConfig.setMainRelationField("t_id");
		fromMapperConfig.setMapperType(ModelType.MANYWAYS);
		fromMapperConfigList.add(fromMapperConfig);
		
		fromMapperConfig = new MapperConfig();
		fromMapperConfig.setMapperName("classMapper");
		fromMapperConfig.setMainRelationField("c_id");
		fromMapperConfig.setMapperType(ModelType.MANYWAYS);
		fromMapperConfigList.add(fromMapperConfig);
	}
	
}
