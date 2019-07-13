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

package org.apache.rocketmq.connect.replicator.connector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.connect.replicator.Config;
import org.apache.rocketmq.connect.replicator.ErrorCode;
import org.apache.rocketmq.connect.replicator.Replicator;
import org.apache.rocketmq.connect.replicator.pattern.BrokerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.exception.DataConnectException;
import io.openmessaging.connector.api.source.SourceTask;

public class ReplicatorSourceTask extends SourceTask {

	private static final Logger log = LoggerFactory.getLogger(ReplicatorSourceTask.class);

	private Replicator replicator;

	private Config config;

	private ByteBuffer sourcePartition;

	@Override
	public void start(KeyValue config) {
		try {
			this.config = new Config();
			this.config.load(config);

			this.sourcePartition = ByteBuffer.wrap(this.config.getNameServerAddress().getBytes("UTF-8"));

			this.replicator = new Replicator(this.config);
			this.replicator.start();
		} catch (Exception e) {
			log.error("activemq task start failed.", e);
			throw new DataConnectException(ErrorCode.START_ERROR_CODE, e.getMessage(), e);
		}

	}

	@Override
	public void stop() {
		try {
			replicator.stop();
		} catch (Exception e) {
			log.error("activemq task stop failed.", e);
			throw new DataConnectException(ErrorCode.STOP_ERROR_CODE, e.getMessage(), e);
		}

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {

	}

	@Override
	public Collection<SourceDataEntry> poll() {
		List<SourceDataEntry> res = new ArrayList<>();

		try {
			BrokerInfo message = replicator.getQueue().poll(1000, TimeUnit.MILLISECONDS);

			if (message != null) {
				Object[] payload = new Object[] { message };
				SourceDataEntry sourceDataEntry = new SourceDataEntry(sourcePartition, null, System.currentTimeMillis(),
						EntryType.CREATE, null, null, payload);
				res.add(sourceDataEntry);
			}
		} catch (Exception e) {
			log.error("activemq task poll error, current config:" + JSON.toJSONString(config), e);
		}
		return res;
	}

}
