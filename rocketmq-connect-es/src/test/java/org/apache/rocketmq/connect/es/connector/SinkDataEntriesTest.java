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

import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;

public class SinkDataEntriesTest {
	

	public static void createRelationSinkDataEntry(List<SinkDataEntry> sinkDataEntries) {
		Schema schema= new Schema();
		schema.setName("relation");
		List<Field> fields = new ArrayList<Field>();
		Field field = new Field(0, "relation_id", FieldType.INT64);
		fields.add(field);
		
		field = new Field(1, "s_id", FieldType.INT64);
		fields.add(field);
		
		field = new Field(2, "s_name", FieldType.STRING);
		fields.add(field);
		
		field = new Field(3, "t_id", FieldType.INT32);
		fields.add(field);
		
		field = new Field(3, "t_name", FieldType.STRING);
		fields.add(field);
		
		field = new Field(3, "c_id", FieldType.INT32);
		fields.add(field);
		
		field = new Field(3, "c_name", FieldType.STRING);
		fields.add(field);
		
		schema.setFields(fields);
		
		Object[] payload = new Object[7];
		payload[ 0 ] = new Object[] {1,1};
		payload[ 1 ] = new Object[] {1,1};
		payload[ 2 ] = new Object[] {"jack","jack1"};
		payload[ 3 ] = new Object[] {1,1};
		payload[ 4 ] = new Object[] {"tommit","tommit1"};
		payload[ 5 ] = new Object[] {1,1};
		payload[ 6 ] = new Object[] {"一年级二班","二年级二班"};
		
		SinkDataEntry sinkDataEntry = new SinkDataEntry(1L, 1L, EntryType.CREATE, null, schema, payload);
		sinkDataEntries.add(sinkDataEntry);
	}
	
	public static void createClassSinkDataEntry(List<SinkDataEntry> sinkDataEntries) {
		Schema schema= new Schema();
		schema.setName("class");
		List<Field> fields = new ArrayList<Field>();
		Field field = new Field(0, "id", FieldType.INT64);
		fields.add(field);
		
		field = new Field(1, "name", FieldType.STRING);
		fields.add(field);
		
		field = new Field(2, "addree", FieldType.STRING);
		fields.add(field);
		
		field = new Field(3, "numberOfPeople", FieldType.INT32);
		fields.add(field);
		schema.setFields(fields);
		
		Object[] payload = new Object[4];
		payload[ 0 ] = new Object[] {1,1};
		payload[ 1 ] = new Object[] {"一年级二班","二年级二班"};
		payload[ 2 ] = new Object[] {"二楼011","三楼011"};
		payload[ 3 ] = new Object[] {100,120};
		
		SinkDataEntry sinkDataEntry = new SinkDataEntry(1L, 1L, EntryType.CREATE, null, schema, payload);
		sinkDataEntries.add(sinkDataEntry);
	}
	
	public static void createTeacherSinkDataEntry(List<SinkDataEntry> sinkDataEntries) {
		Schema schema= new Schema();
		schema.setName("teacher");
		List<Field> fields = new ArrayList<Field>();
		Field field = new Field(0, "id", FieldType.INT64);
		fields.add(field);
		
		field = new Field(0, "name", FieldType.STRING);
		fields.add(field);
		
		field = new Field(0, "subject", FieldType.STRING);
		fields.add(field);
		
		field = new Field(0, "sex", FieldType.BYTES);
		fields.add(field);
		schema.setFields(fields);
		
		Object[] payload = new Object[4];
		payload[ 0 ] = new Object[] {1,1};
		payload[ 1 ] = new Object[] {"tommit","tommit1"};
		payload[ 2 ] = new Object[] {"语文老师","语文老师"};
		payload[ 3 ] = new Object[] {1,1};
		
		SinkDataEntry sinkDataEntry = new SinkDataEntry(1L, 1L, EntryType.CREATE, null, schema, payload);
		sinkDataEntries.add(sinkDataEntry);
	}
	
	public static void createStudentSinkDataEntry(List<SinkDataEntry> sinkDataEntries) {
		Schema schema= new Schema();
		schema.setName("student");
		List<Field> fields = new ArrayList<Field>();
		Field field = new Field(0, "id", FieldType.INT64);
		fields.add(field);
		
		field = new Field(1, "name", FieldType.STRING);
		fields.add(field);
		
		field = new Field(2, "age", FieldType.INT64);
		fields.add(field);
		
		field = new Field(3, "sex", FieldType.BYTES);
		fields.add(field);
		
		field = new Field(4, "relation_id", FieldType.INT64);
		fields.add(field);
		schema.setFields(fields);
		
		
		
		
		Object[] payload = new Object[fields.size()];
		payload[ 0 ] = new Object[] {1,1};
		payload[ 1 ] = new Object[] {"jack","jack1"};
		payload[ 2 ] = new Object[] {12,13};
		payload[ 3 ] = new Object[] {1,1};
		payload[ 4 ] = new Object[] {1,1};
		
		SinkDataEntry sinkDataEntry = new SinkDataEntry(1L, 1L, EntryType.CREATE, null, schema, payload);
		sinkDataEntries.add(sinkDataEntry);
	}
	
}
