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

package org.apache.rocketmq.connect.jdbc;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Replicator.class);

    private static final Logger POSITION_LOGGER = LoggerFactory.getLogger("PositionLogger");

    private Config config;

    private EventProcessor eventProcessor;

    private Object lock = new Object();
    private BinlogPosition nextBinlogPosition;
    private long nextQueueOffset;
    private long xid;
    private BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>();

    public Replicator(Config config){
        this.config = config;
    }

    public void start() {

        try {

            eventProcessor = new EventProcessor(this);
            eventProcessor.start();

        } catch (Exception e) {
            LOGGER.error("Start error.", e);
        }
    }

    public void stop(){
        eventProcessor.stop();
    }

    public void commit(Transaction transaction, boolean isComplete) {

        queue.add(transaction);
        for (int i = 0; i < 3; i++) {
            try {
                if (isComplete) {
                    long offset = 1;
                    synchronized (lock) {
                        xid = transaction.getXid();
                        nextBinlogPosition = transaction.getNextBinlogPosition();
                        nextQueueOffset = offset;
                    }

                } else {
                }
                break;

            } catch (Exception e) {
                LOGGER.error("Push error,retry:" + (i + 1) + ",", e);
            }
        }
    }

    public void logPosition() {

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
            POSITION_LOGGER.info("XID: {},   BINLOG_FILE: {},   NEXT_POSITION: {},   NEXT_OFFSET: {}",
                xid, binlogFilename, nextPosition, nextOffset);
        }

    }

    public Config getConfig() {
        return config;
    }

//    public BinlogPosition getNextBinlogPosition() {
//        return nextBinlogPosition;
//    }

    public BlockingQueue<Transaction> getQueue() {
        return queue;
    }
}
