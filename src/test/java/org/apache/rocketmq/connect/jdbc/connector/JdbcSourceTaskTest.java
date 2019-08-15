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

package org.apache.rocketmq.connect.jdbc.connector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import java.sql.*;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;

public class JdbcSourceTaskTest {
	KeyValue kv;
	DataSource dataSource;

	@Test
	public void testBulk() throws InterruptedException {
		KeyValue kv = new DefaultKeyValue();
		kv.put("jdbcUrl", "localhost:3306");
		kv.put("jdbcUsername", "root");
		kv.put("jdbcPassword", "199812160");
		kv.put("mode", "bulk");
		kv.put("rocketmqTopic", "JdbcTopic");
		JdbcSourceTask task = new JdbcSourceTask();
		task.start(kv);
		Collection<SourceDataEntry> sourceDataEntry = task.poll();
		System.out.println(sourceDataEntry);
	}

	@Test
	public void testTimestampIncrementing() throws InterruptedException, SQLException {
		kv = new DefaultKeyValue();
		kv.put("jdbcUrl", "localhost:3306");
		kv.put("jdbcUsername", "root");
		kv.put("jdbcPassword", "199812160");
		kv.put("incrementingColumnName", "id");
		kv.put("timestampColmnName", "timestamp");
		kv.put("mode", "incrementing+timestamp");
		kv.put("rocketmqTopic", "JdbcTopic");
		JdbcSourceTask task = new JdbcSourceTask();
		task.start(kv);
		Collection<SourceDataEntry> sourceDataEntry = task.poll();
		System.out.println(sourceDataEntry);
		Map<String, String> map = new HashMap<>();
		map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
		map.put("url", "jdbc:mysql://" + kv.getString("jdbcUrl")
				+ "?useSSL=true&verifyServerCertificate=false&serverTimezone=GMT%2B8");
		map.put("username", kv.getString("jdbcUsername"));
		map.put("password", kv.getString("jdbcPassword"));
		map.put("initialSize", "2");
		map.put("maxActive", "2");
		map.put("maxWait", "60000");
		map.put("timeBetweenEvictionRunsMillis", "60000");
		map.put("minEvictableIdleTimeMillis", "300000");
		map.put("validationQuery", "SELECT 1 FROM DUAL");
		map.put("testWhileIdle", "true");
		try {
			dataSource = DruidDataSourceFactory.createDataSource(map);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Connection connection= dataSource.getConnection();
		PreparedStatement statement;
		String s="insert into time_db.timestamp_tb (name) values(\"test\")";
		statement=connection.prepareStatement(s);
		statement.executeUpdate();

		sourceDataEntry = task.poll();
		System.out.println(sourceDataEntry);
		s="update time_db.timestamp_tb set name=\"liu\" where id < 2";
		statement=connection.prepareStatement(s);
		statement.executeUpdate();
		sourceDataEntry = task.poll();
		System.out.println(sourceDataEntry);
		task.stop();
		
		connection.close();
	}
}
