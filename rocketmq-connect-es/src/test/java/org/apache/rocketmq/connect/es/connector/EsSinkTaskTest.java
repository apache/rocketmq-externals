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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.rocketmq.connect.es.Config;
import org.apache.rocketmq.connect.es.config.ConfigManage;
import org.junit.Before;
import org.junit.Test;

import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.SinkDataEntry;

public class EsSinkTaskTest {

	EsSinkTask esSinkTask = new EsSinkTask();

	List<SinkDataEntry> sinkDataEntries = new ArrayList<>();
	
	ConfigManage configManage = new ConfigManage();
	
	
	@Before
	public void init() throws IllegalAccessException {
		//添加学生
		SinkDataEntriesTest.createStudentSinkDataEntry(sinkDataEntries);
		SinkDataEntriesTest.createTeacherSinkDataEntry(sinkDataEntries);
		SinkDataEntriesTest.createClassSinkDataEntry(sinkDataEntries);
		SinkDataEntriesTest.createRelationSinkDataEntry(sinkDataEntries);
		FieldUtils.writeDeclaredField(esSinkTask, "configManage", configManage, true);
		
		Config config = new Config(configManage);
		config.handle(ConfigInfo.simulation());
		config.getClass();
	}
	

	@Test
	public void startTest() {
		esSinkTask.put(sinkDataEntries);
	}

	@Test
	public void update() {
		for(SinkDataEntry sinkDataEntry : sinkDataEntries) {
			sinkDataEntry.setEntryType(EntryType.UPDATE);
		}
		esSinkTask.put(sinkDataEntries);
	}
	

}
