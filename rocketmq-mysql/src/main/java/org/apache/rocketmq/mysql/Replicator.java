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

package org.apache.rocketmq.mysql;

import org.apache.rocketmq.mysql.binlog.EventProcessor;
import org.apache.rocketmq.mysql.binlog.Transaction;
import org.apache.rocketmq.mysql.offset.OffsetLogThread;
import org.apache.rocketmq.mysql.productor.RocketMQProducer;
import org.apache.rocketmq.mysql.position.BinlogPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Replicator.class);

    private static final Logger OFFSET_LOGGER = LoggerFactory.getLogger("OffsetLogger");

    private Config config;

    private EventProcessor eventProcessor;

    private RocketMQProducer rocketMQProducer;

    private Object lock = new Object();
    private BinlogPosition nextBinlogPosition;
    private long nextQueueOffset;
    private long xid;

    public static void main(String[] args) {

        Replicator replicator = new Replicator();
        replicator.start();
    }

    public void start() {

        try {
            config = new Config();
            config.load();

            rocketMQProducer = new RocketMQProducer(config);
            rocketMQProducer.start();

            OffsetLogThread offsetLogThread = new OffsetLogThread(this);
            offsetLogThread.start();

            eventProcessor = new EventProcessor(this);
            eventProcessor.start();

        } catch (Exception e) {
            LOGGER.error("Start error.", e);
            System.exit(1);
        }
    }

    public void commit(Transaction transaction, boolean isComplete) {

        String json = transaction.toJson();

        for (int i = 0; i < 3; i++) {
            try {
                if (isComplete) {
                    long offset = rocketMQProducer.push(json);

                    synchronized (lock) {
                        xid = transaction.getXid();
                        nextBinlogPosition = transaction.getNextBinlogPosition();
                        nextQueueOffset = offset;
                    }

                } else {
                    rocketMQProducer.push(json);
                }
                break;

            } catch (Exception e) {
                LOGGER.error("Push error,retry:" + (i + 1) + ",", e);
            }
        }
    }

    public void logOffset() {

        String binlogFilename = null;
        long xid = 0L;
        long nextPosition = 0L;
        long nextOffset = 0L;

        synchronized (lock) {
            if (nextBinlogPosition != null) {
                xid = this.xid;
                binlogFilename = nextBinlogPosition.getBinlogFilename();
                nextPosition = nextBinlogPosition.getPosition();
                nextOffset = nextQueueOffset;
            }
        }

        if (binlogFilename != null) {
            OFFSET_LOGGER.info("XID: {},   BINLOG_FILE: {},   NEXT_POSITION: {},   NEXT_OFFSET: {}",
                xid, binlogFilename, nextPosition, nextOffset);
        }

    }

    public Config getConfig() {
        return config;
    }

    public BinlogPosition getNextBinlogPosition() {
        return nextBinlogPosition;
    }

}
